package com.ra.base_spring_boot.services.song;

import com.ra.base_spring_boot.dto.req.NotificationRequest;
import com.ra.base_spring_boot.dto.req.SongRequestDTO;
import com.ra.base_spring_boot.dto.resp.AlbumResponseDTO;
import com.ra.base_spring_boot.dto.resp.ArtistResponseDTO;
import com.ra.base_spring_boot.dto.resp.CommentResponseDTO;
import com.ra.base_spring_boot.dto.resp.SongDetailDTO;
import com.ra.base_spring_boot.dto.resp.SongResponseDTO;
import com.ra.base_spring_boot.exception.HttpBadRequest;
import com.ra.base_spring_boot.model.Comment;
import com.ra.base_spring_boot.model.Genre;
import com.ra.base_spring_boot.model.Song;
import com.ra.base_spring_boot.model.User;
import com.ra.base_spring_boot.model.constants.ApprovalStatus;
import com.ra.base_spring_boot.repository.AlbumRepository;
import com.ra.base_spring_boot.repository.CommentRepository;
import com.ra.base_spring_boot.repository.GenreRepository;
import com.ra.base_spring_boot.repository.SongRepository;
import com.ra.base_spring_boot.repository.IUserRepository;
import com.ra.base_spring_boot.services.impl.CloudinaryService;
import com.ra.base_spring_boot.services.user.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.sql.Time;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import com.ra.base_spring_boot.exception.HttpUnAuthorized;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import com.mpatric.mp3agic.Mp3File;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import com.ra.base_spring_boot.services.notification.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RequiredArgsConstructor
@Service
public class SongServiceImpl implements SongService {
    private final SongRepository songRepository;
    private final GenreRepository genreRepository;
    private final IUserRepository userRepository;
    private final CloudinaryService cloudinaryService;
    private final AlbumRepository albumRepository;
    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final CommentRepository commentRepository;
    private final NotificationService notificationService;
    private final UserService userService;

    private static final Logger logger = LoggerFactory.getLogger(SongServiceImpl.class);

    @Override
    @Transactional
    public SongResponseDTO addSong(SongRequestDTO dto, Authentication authentication) {
        Long artistId = getUserIdFromAuthentication(authentication);
        if (artistId == null) {
            throw new HttpUnAuthorized("Access denied. Artist role required.");
        }

        User user = userRepository.findById(artistId)
                .orElseThrow(() -> new HttpBadRequest("Artist not found"));

        if (dto.getGenreIds() == null || dto.getGenreIds().isEmpty()) {
            throw new HttpBadRequest("At least one genre must be specified");
        }

        List<Genre> genres = genreRepository.findAllById(dto.getGenreIds());
        if (genres.size() != dto.getGenreIds().size()) {
            throw new HttpBadRequest("One or more specified genres do not exist");
        }

        String fileUrl = cloudinaryService.uploadFile(dto.getFile(), "audio");
        String imageUrl = cloudinaryService.uploadFile(dto.getProfilePic(), "images");

        Duration duration = getDuration(dto.getFile());
        Time durationTime = convertDurationToTime(duration);

        Song song = Song.builder()
                .title(dto.getTitle())
                .duration(durationTime)
                .albums(null)
                .fileUrl(fileUrl)
                .profilePicUrl(imageUrl)
                .users(user)
                .genres(genres)
                .status(com.ra.base_spring_boot.model.constants.ApprovalStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .views(0L)
                .lyrics(dto.getLyrics())
                .build();

        Song savedSong = songRepository.save(song);
        return convertToDTO(savedSong);
    }

    @Override
    @Transactional
    public SongResponseDTO updateSong(Long songId, SongRequestDTO dto, Authentication authentication) {
        Song song = songRepository.findById(songId)
                .orElseThrow(() -> new HttpBadRequest("Song not found"));

        Long currentArtistId = getUserIdFromAuthentication(authentication);
        if (!song.getUsers().getId().equals(currentArtistId)) {
            throw new HttpBadRequest("Permission denied. Only song owner can update.");
        }

        User user = userRepository.findById(currentArtistId)
                .orElseThrow(() -> new HttpBadRequest("Artist not found"));

        List<Genre> genres = genreRepository.findAllById(dto.getGenreIds());
        if (genres.isEmpty()) {
            throw new HttpBadRequest("Genres not found");
        }

        song.setTitle(dto.getTitle());
        song.setUsers(user);
        song.setGenres(genres);
        song.setUpdatedAt(LocalDateTime.now());
        song.setProfilePicUrl(cloudinaryService.uploadFile(dto.getProfilePic(), "images"));
        song.setLyrics(dto.getLyrics());

        return convertToDTO(songRepository.save(song));
    }


    @Override
    @Transactional
    public void deleteByArtist(Long songId) {
        Song song = songRepository.findById(songId)
                .orElseThrow(() -> new HttpBadRequest("Song not found"));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long currentUserId = getUserIdFromAuthentication(authentication);

        if (!song.getUsers().getId().equals(currentUserId)) {
            throw new HttpBadRequest("Permission denied. Only song owner can delete.");
        }

        song.setStatus(ApprovalStatus.DELETED_BY_ARTIST);
        song.setUpdatedAt(LocalDateTime.now());
        songRepository.save(song);
    }

    @Override
    public List<SongResponseDTO> getApprovedSongsByAlbum(Long albumId) {
        List<Song> songs = songRepository.findApprovedSongsByAlbumId(albumId);
        return songs.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void removeSongFromAlbum(Long songId) {
        Song song = songRepository.findById(songId)
                .orElseThrow(() -> new HttpBadRequest("Song not found"));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long currentUserId = getUserIdFromAuthentication(authentication);

        if (!song.getUsers().getId().equals(currentUserId)) {
            throw new HttpBadRequest("Permission denied. Only song owner can remove from album.");
        }

        song.setAlbums(null);
        song.setUpdatedAt(LocalDateTime.now());
        songRepository.save(song);
    }

    @Override
    @Transactional
    public void addSongToAlbum(Long songId, Long albumId) {
        Song song = songRepository.findById(songId)
                .orElseThrow(() -> new HttpBadRequest("Song not found"));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long currentUserId = getUserIdFromAuthentication(authentication);

        if (!song.getUsers().getId().equals(currentUserId)) {
            throw new HttpBadRequest("Permission denied. Only song owner can add to album.");
        }

        var album = albumRepository.findById(albumId)
                .orElseThrow(() -> new HttpBadRequest("Album not found"));
        song.setAlbums(album);
        song.setUpdatedAt(LocalDateTime.now());
        songRepository.save(song);
    }

    @Override
    public List<SongResponseDTO> getAllSongs() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getAuthorities().stream()
                .anyMatch(r -> r.getAuthority().equals("ROLE_ADMIN"))) {
            List<Song> songs = songRepository.findAll();
            return songs.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        } else {
            List<Song> songs = songRepository.findAll();
            return songs.stream()
                    .filter(song -> song.getStatus() == ApprovalStatus.APPROVED)
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        }
    }

    @Override
    public Long countSong(LocalDateTime start, LocalDateTime end) {
        if (start == null && end == null) {
            return songRepository.count();
        }
        if (start != null && end != null && start.isAfter(end)) {
            throw new IllegalArgumentException("Start time cannot be after end time");
        }
        return songRepository.countSongByDate(start, end);
    }

    @Override
    public User getUserByUsername(String username) {
        return userRepository.findByEmail(username).orElse(null);
    }

    @Override
    public List<SongResponseDTO> getMySongs(Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        if (userId == null) {
            throw new HttpUnAuthorized("Access denied. Artist role required.");
        }
        return getSongsByUserId(userId);
    }

    private List<SongResponseDTO> getSongsByUserId(Long userId) {
        List<Song> songs = songRepository.findByUsers_Id(userId);
        return songs.stream()
                .filter(song -> song.getStatus() == ApprovalStatus.APPROVED)
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<SongResponseDTO> getMyRejectedSongs(Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        if (userId == null) {
            throw new HttpUnAuthorized("Access denied. Artist role required.");
        }
        List<Song> rejectedSongs = songRepository.findByUsers_IdAndStatus(userId, ApprovalStatus.REJECTED);
        return rejectedSongs.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<SongResponseDTO> getMySelfDeletedSongs(Authentication authentication) {
        Long userId = null;
        boolean isArtist = false;

        if (authentication != null) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserDetails userDetails) {
                User user = getUserByUsername(userDetails.getUsername());
                userId = user.getId();
                isArtist = user.getRoles().stream().anyMatch(r ->
                        r.getRoleName() == com.ra.base_spring_boot.model.constants.RoleName.ROLE_ARTIST ||
                                r.getRoleName() == com.ra.base_spring_boot.model.constants.RoleName.ROLE_ADMIN ||
                                r.getRoleName().name().equals("ARTIST") ||
                                r.getRoleName().name().equals("ADMIN")
                );
            } else if (principal instanceof User user) {
                userId = user.getId();
                isArtist = user.getRoles().stream().anyMatch(r ->
                        r.getRoleName() == com.ra.base_spring_boot.model.constants.RoleName.ROLE_ARTIST ||
                                r.getRoleName() == com.ra.base_spring_boot.model.constants.RoleName.ROLE_ADMIN ||
                                r.getRoleName().name().equals("ARTIST") ||
                                r.getRoleName().name().equals("ADMIN")
                );
            }
        }

        if (userId == null || !isArtist) {
            throw new HttpUnAuthorized("Access denied. Artist role required.");
        }

        List<Song> deletedSongs = songRepository.findByUsers_IdAndStatus(userId, com.ra.base_spring_boot.model.constants.ApprovalStatus.DELETED_BY_ARTIST);
        return deletedSongs.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    public List<SongResponseDTO> getMyDeletedByAdminSongs(Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        if (userId == null) {
            throw new HttpUnAuthorized("Access denied. Artist role required.");
        }

        List<Song> deletedByAdminSongs = songRepository.findByUsers_IdAndStatus(userId, ApprovalStatus.DELETED_BY_ADMIN);
        return deletedByAdminSongs.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void requestRestoreSong(Long songId) {
        Song song = songRepository.findById(songId)
                .orElseThrow(() -> new HttpBadRequest("Song not found"));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long currentUserId = getUserIdFromAuthentication(authentication);

        if (!song.getUsers().getId().equals(currentUserId)) {
            throw new HttpBadRequest("Permission denied. Only song owner can request restoration.");
        }

        if (song.getStatus() != ApprovalStatus.REJECTED &&
                song.getStatus() != ApprovalStatus.DELETED_BY_ADMIN &&
                song.getStatus() != ApprovalStatus.DELETED_BY_ARTIST) {
            throw new HttpBadRequest("Only songs with Inappropriate Content or Deleted status can be requested for restoration");
        }

        song.setStatus(com.ra.base_spring_boot.model.constants.ApprovalStatus.PENDING);
        song.setUpdatedAt(LocalDateTime.now());
        songRepository.save(song);
    }

    @Override
    public List<SongResponseDTO> getPendingSongs() {
        List<Song> songs = songRepository.findByStatus(com.ra.base_spring_boot.model.constants.ApprovalStatus.PENDING);
        return songs.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    public List<SongResponseDTO> getRejectedSongs() {
        List<Song> songs = songRepository.findByStatus(com.ra.base_spring_boot.model.constants.ApprovalStatus.REJECTED);
        return songs.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    public List<SongResponseDTO> getDeletedByArtistSongs() {
        List<Song> songs = songRepository.findByStatus(ApprovalStatus.DELETED_BY_ARTIST);
        return songs.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    public List<SongResponseDTO> getDeletedByAdminSongs() {
        List<Song> songs = songRepository.findByStatus(ApprovalStatus.DELETED_BY_ADMIN);
        return songs.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void approveSong(Long songId) {
        Song song = songRepository.findById(songId)
                .orElseThrow(() -> new HttpBadRequest("Song not found"));
        song.setStatus(com.ra.base_spring_boot.model.constants.ApprovalStatus.APPROVED);
        song.setUpdatedAt(LocalDateTime.now());
        songRepository.save(song);

        notificationService.notifyArtistSongApproved(song.getUsers().getId(), song.getTitle());
        List<Long> followerIds = userService.getFollowerIds(userService, song.getUsers().getId());
        notificationService.notifySongApproved(song.getUsers().getId(), song.getTitle(), followerIds);
    }

    @Override
    @Transactional
    public void rejectSong(Long songId, String reason) {
        Song song = songRepository.findById(songId)
                .orElseThrow(() -> new HttpBadRequest("Song not found"));
        song.setStatus(ApprovalStatus.REJECTED);
        song.setUpdatedAt(LocalDateTime.now());
        songRepository.save(song);
        try {
            Context context = new Context();
            String artistName = song.getUsers().getFirstName() + " " + song.getUsers().getLastName();
            context.setVariable("artistName", artistName);
            context.setVariable("songTitle", song.getTitle());
            context.setVariable("genre", song.getGenres().stream()
                    .map(Genre::getGenreName)
                    .collect(Collectors.joining(", ")));
            context.setVariable("releaseDate", song.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            context.setVariable("rejectTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
            context.setVariable("reason", reason);

            String emailContent = templateEngine.process("email/song-rejection-notification", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(song.getUsers().getEmail());
            helper.setSubject("Song Rejection Notification - " + song.getTitle());
            helper.setText(emailContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            logger.error("Error sending email: " + e.getMessage(), e);
        }
        String message = "Your song '" + song.getTitle() + "' was rejected. Reason: " + reason;
        notificationService.createNotification(new NotificationRequest(song.getUsers().getId(), message, "song_rejected"));
    }

    @Override
    @Transactional
    public void restoreSong(Long songId) {
        Song song = songRepository.findById(songId)
                .orElseThrow(() -> new HttpBadRequest("Song not found"));
        song.setStatus(ApprovalStatus.APPROVED);
        song.setUpdatedAt(LocalDateTime.now());
        songRepository.save(song);

        try {
            Context context = new Context();
            String artistName = song.getUsers().getFirstName() + " " + song.getUsers().getLastName();
            context.setVariable("artistName", artistName);
            context.setVariable("songTitle", song.getTitle());
            context.setVariable("genre", song.getGenres().stream()
                    .map(Genre::getGenreName)
                    .collect(Collectors.joining(", ")));
            context.setVariable("releaseDate", song.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            context.setVariable("restorationTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));

            String emailContent = templateEngine.process("email/song-restoration-notification", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(song.getUsers().getEmail());
            helper.setSubject("Song Restoration Notification - " + song.getTitle());
            helper.setText(emailContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            logger.error("Error sending email: " + e.getMessage(), e);
        }
    }


    @Override
    @Transactional
    public void deleteSongByAdmin(Long id, String reason) {
        Song song = songRepository.findById(id)
                .orElseThrow(() -> new HttpBadRequest("Song not found"));
        song.setStatus(ApprovalStatus.DELETED_BY_ADMIN);
        song.setUpdatedAt(LocalDateTime.now());
        songRepository.save(song);

        try {
            Context context = new Context();
            String artistName = song.getUsers().getFirstName() + " " + song.getUsers().getLastName();
            context.setVariable("artistName", artistName);
            context.setVariable("songTitle", song.getTitle());
            context.setVariable("reason", reason);
            context.setVariable("genre", song.getGenres().stream()
                    .map(Genre::getGenreName)
                    .collect(Collectors.joining(", ")));
            context.setVariable("deletionTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));

            String emailContent = templateEngine.process("email/song-deletion-notification", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(song.getUsers().getEmail());
            helper.setSubject("Song Deletion Notification - " + song.getTitle());
            helper.setText(emailContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            logger.error("Error sending email: " + e.getMessage(), e);
        }
    }


    @Override
    public SongDetailDTO getSongDetailForGuest(Long songId) {
        Song song = songRepository.findById(songId)
                .orElseThrow(() -> new HttpBadRequest("Song not found"));
        User artist = song.getUsers();
        List<Comment> comments = commentRepository.findBySongsIdOrderByCreatedAtDesc(songId);

        SongResponseDTO songDTO = convertToDTO(song);
        List<CommentResponseDTO> commentDTOs = comments.stream()
                .map(this::convertCommentToDTO)
                .collect(Collectors.toList());

        AlbumResponseDTO albumDTO = null;
        if (song.getAlbums() != null) {
            albumDTO = AlbumResponseDTO.builder()
                    .id(song.getAlbums().getId())
                    .titleAlbum(song.getAlbums().getTitle())
                    .releaseDateStr(song.getAlbums().getReleaseDate() != null ? song.getAlbums().getReleaseDate().toString() : null)
                    .artistId(song.getAlbums().getUsers() != null ? song.getAlbums().getUsers().getId().intValue() : null)
                    .coverImageUrl(song.getAlbums().getCoverImage())
                    .type(song.getAlbums().getType())
                    .createdAtStr(song.getAlbums().getCreatedAt() != null ? song.getAlbums().getCreatedAt().toString() : null)
                    .updatedAtStr(song.getAlbums().getUpdatedAt() != null ? song.getAlbums().getUpdatedAt().toString() : null)
                    .build();
        }

        return SongDetailDTO.builder()
                .song(songDTO)
                .artist(convertToArtistResponseDTO(artist))
                .album(albumDTO)
                .comments(commentDTOs)
                .build();
    }

    @Override
    public SongDetailDTO getSongDetailWithAuth(Long songId, Authentication authentication) {
        Song song = songRepository.findById(songId)
                .orElseThrow(() -> new HttpBadRequest("Song not found"));
        User artist = song.getUsers();
        List<Comment> comments = commentRepository.findBySongsIdOrderByCreatedAtDesc(songId);

        SongResponseDTO songDTO = convertToDTO(song);
        List<CommentResponseDTO> commentDTOs = comments.stream()
                .map(this::convertCommentToDTO)
                .collect(Collectors.toList());

        AlbumResponseDTO albumDTO = null;
        if (song.getAlbums() != null) {
            albumDTO = AlbumResponseDTO.builder()
                    .id(song.getAlbums().getId())
                    .titleAlbum(song.getAlbums().getTitle())
                    .releaseDateStr(song.getAlbums().getReleaseDate() != null ? song.getAlbums().getReleaseDate().toString() : null)
                    .artistId(song.getAlbums().getUsers() != null ? song.getAlbums().getUsers().getId().intValue() : null)
                    .coverImageUrl(song.getAlbums().getCoverImage())
                    .type(song.getAlbums().getType())
                    .createdAtStr(song.getAlbums().getCreatedAt() != null ? song.getAlbums().getCreatedAt().toString() : null)
                    .updatedAtStr(song.getAlbums().getUpdatedAt() != null ? song.getAlbums().getUpdatedAt().toString() : null)
                    .build();
        }

        return SongDetailDTO.builder()
                .song(songDTO)
                .artist(convertToArtistResponseDTO(artist))
                .album(albumDTO)
                .comments(commentDTOs)
                .build();
    }

    private ArtistResponseDTO convertToArtistResponseDTO(User user) {
        return ArtistResponseDTO.builder()
                .artistId(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .profile_image(user.getProfileImage())
                .bio(user.getBio())
                .status(user.getStatus() != null ? user.getStatus().name() : null)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    private CommentResponseDTO convertCommentToDTO(Comment comment) {
        String createdAtStr = comment.getCreatedAt() != null ? comment.getCreatedAt().toString() : null;
        String updatedAtStr = comment.getUpdatedAt() != null ? comment.getUpdatedAt().toString() : null;
        Long parentId = comment.getParentComment() != null ? comment.getParentComment().getId() : null;
        return CommentResponseDTO.builder()
                .id(comment.getId())
                .userId(comment.getUsers() != null ? comment.getUsers().getId() : null)
                .username(comment.getUsers() != null ? comment.getUsers().getFirstName() + " " + comment.getUsers().getLastName() : null)
                .songId(comment.getSongs() != null ? comment.getSongs().getId() : null)
                .content(comment.getContent())
                .parentId(parentId)
                .createdAt(createdAtStr)
                .updatedAt(updatedAtStr)
                .replies(null)
                .reactionCounts(null)
                .currentUserReaction(null)
                .build();
    }
    @Override
    public Long getUserIdFromAuthentication(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserDetails userDetails) {
                String username = userDetails.getUsername();
                User user = getUserByUsername(username);
                if (user != null) {
                    return user.getId();
                }
            } else if (principal instanceof User user) {
                return user.getId();
            }
        }
        throw new HttpUnAuthorized("Authentication failed.");
    }

    private Time convertDurationToTime(Duration duration) {
        if (duration == null) throw new HttpBadRequest("Invalid audio file");
        long seconds = duration.getSeconds();
        return Time.valueOf(String.format("%02d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, seconds % 60));
    }

    private Duration getDuration(MultipartFile file) {
        File temp = null;
        try {
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || !originalFilename.contains(".")) {
                throw new HttpBadRequest("Invalid audio file name");
            }

            String ext = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
            temp = File.createTempFile("temp", ext);
            file.transferTo(temp);

            try {
                if (ext.equals(".mp3")) {
                    Mp3File mp3file = new Mp3File(temp);
                    long durationInSeconds = mp3file.getLengthInSeconds();
                    if (!temp.delete()) {
                        System.err.println("Warning: Failed to delete temporary file: " + temp.getAbsolutePath());
                    }
                    return Duration.ofSeconds(durationInSeconds);
                } else {
                    AudioInputStream ais = AudioSystem.getAudioInputStream(temp);
                    AudioFormat format = ais.getFormat();
                    long frames = ais.getFrameLength();
                    double durationInSeconds = (frames + 0.0) / format.getFrameRate();
                    if (!temp.delete()) {
                        System.err.println("Warning: Failed to delete temporary file: " + temp.getAbsolutePath());
                    }
                    return Duration.ofSeconds((long) durationInSeconds);
                }
            } catch (Exception e) {
                System.out.println("Mp3agic failed, trying javax.sound.sampled: " + e.getMessage());
                try {
                    AudioInputStream ais = AudioSystem.getAudioInputStream(temp);
                    AudioFormat format = ais.getFormat();
                    long frames = ais.getFrameLength();
                    double durationInSeconds = (frames + 0.0) / format.getFrameRate();
                    if (!temp.delete()) {
                        System.err.println("Warning: Failed to delete temporary file: " + temp.getAbsolutePath());
                    }
                    return Duration.ofSeconds((long) durationInSeconds);
                } catch (Exception e2) {
                    if (!temp.delete()) {
                        System.err.println("Warning: Failed to delete temporary file: " + temp.getAbsolutePath());
                    }
                    throw new HttpBadRequest("Unsupported audio format or corrupted file. Error: " + e2.getMessage());
                }
            }
        } catch (Exception e) {
            if (temp != null && temp.exists()) {
                if (!temp.delete()) {
                    System.err.println("Warning: Failed to delete temporary file: " + temp.getAbsolutePath());
                }
            }
            throw new HttpBadRequest("Audio error: " + e.getMessage());
        }
    }
    private SongResponseDTO convertToDTO(Song song) {
        Long albumId = null;
        String albumName = null;
        if (song.getAlbums() != null) {
            albumId = song.getAlbums().getId();
            albumName = song.getAlbums().getTitle();
        }
        String artistName = null;
        if (song.getUsers() != null) {
            String firstName = song.getUsers().getFirstName();
            String lastName = song.getUsers().getLastName();
            String tempArtistName = (firstName != null ? firstName : "") + (lastName != null ? (firstName != null ? " " : "") + lastName : "");
            artistName = tempArtistName.trim().isEmpty() ? null : tempArtistName.trim();
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        String createdAtStr = song.getCreatedAt() != null ? song.getCreatedAt().format(formatter) : null;
        String updatedAtStr = song.getUpdatedAt() != null ? song.getUpdatedAt().format(formatter) : null;

        return SongResponseDTO.builder()
                .id(song.getId())
                .title(song.getTitle())
                .duration(song.getDuration())
                .artistId(song.getUsers() != null ? song.getUsers().getId() : 0)
                .artistName(artistName)
                .profilePicUrl(song.getProfilePicUrl())
                .fileUrl(song.getFileUrl())
                .albumId(albumId)
                .albumName(albumName)
                .views(song.getViews())
                .status(song.getStatus() != null ? song.getStatus().name() : null)
                .createdAtStr(createdAtStr)
                .updatedAtStr(updatedAtStr)
                .build();
    }

    @Override
    public String getLyricById(Long songId) {
        return songRepository.findById(songId)
                .map(Song::getLyrics)
                .map(lyric -> lyric.isEmpty() ? "Currently, the lyrics for this song have not been updated by the author" : lyric)
                .orElseThrow(() -> new HttpBadRequest("Song not found"));
    }

    @Override
    public List<SongResponseDTO> getSongsByArtistId(Long artistId) {
        List<Song> songs = songRepository.findByUsers_Id(artistId);
        return songs.stream().map(this::convertToSongResponseDTO).collect(Collectors.toList());
    }

    private SongResponseDTO convertToSongResponseDTO(Song song) {
        Long albumId = null;
        String albumName = null;
        if (song.getAlbums() != null) {
            albumId = song.getAlbums().getId();
            albumName = song.getAlbums().getTitle();
        }
        String artistName = null;
        if (song.getUsers() != null) {
            String firstName = song.getUsers().getFirstName();
            String lastName = song.getUsers().getLastName();
            String tempArtistName = (firstName != null ? firstName : "") + (lastName != null ? (firstName != null ? " " : "") + lastName : "");
            artistName = tempArtistName.trim().isEmpty() ? null : tempArtistName.trim();
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        String createdAtStr = song.getCreatedAt() != null ? song.getCreatedAt().format(formatter) : null;
        String updatedAtStr = song.getUpdatedAt() != null ? song.getUpdatedAt().format(formatter) : null;
        return SongResponseDTO.builder()
                .id(song.getId())
                .title(song.getTitle())
                .duration(song.getDuration())
                .artistId(song.getUsers() != null ? song.getUsers().getId() : 0)
                .artistName(artistName)
                .profilePicUrl(song.getProfilePicUrl())
                .fileUrl(song.getFileUrl())
                .albumId(albumId)
                .albumName(albumName)
                .views(song.getViews())
                .status(song.getStatus() != null ? song.getStatus().name() : null)
                .createdAtStr(createdAtStr)
                .updatedAtStr(updatedAtStr)
                .build();
    }
    @Override
    public List<SongResponseDTO> fullTextSearchSong(String keyword) {
        List<Song> songList = songRepository.fullTextSearchSongs(keyword);
        return songList.stream().map(this::convertToSongResponseDTO).collect(Collectors.toList());
    }


    @Override
    public List<SongResponseDTO> getMyPendingSongs(Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        if (userId == null) {
            throw new HttpUnAuthorized("Access denied. Artist role required.");
        }
        List<Song> pendingSongs = songRepository.findByUsers_IdAndStatus(userId, ApprovalStatus.PENDING);
        return pendingSongs.stream().map(this::convertToDTO).collect(Collectors.toList());
    }
}
