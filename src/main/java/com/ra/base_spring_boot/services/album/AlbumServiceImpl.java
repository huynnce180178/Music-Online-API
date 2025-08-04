package com.ra.base_spring_boot.services.album;

import com.ra.base_spring_boot.dto.req.AlbumRequestDTO;
import com.ra.base_spring_boot.dto.resp.*;
import com.ra.base_spring_boot.exception.HttpNotFound;
import com.ra.base_spring_boot.exception.HttpUnAuthorized;
import com.ra.base_spring_boot.exception.HttpBadRequest;
import com.ra.base_spring_boot.model.Album;
import com.ra.base_spring_boot.model.User;
import com.ra.base_spring_boot.model.constants.ApprovalStatus;
import com.ra.base_spring_boot.repository.AlbumRepository;
import com.ra.base_spring_boot.repository.IUserRepository;
import com.ra.base_spring_boot.repository.SongRepository;
import com.ra.base_spring_boot.services.impl.CloudinaryService;
import com.ra.base_spring_boot.services.notification.NotificationService;
import com.ra.base_spring_boot.services.song.SongService;
import com.ra.base_spring_boot.services.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AlbumServiceImpl implements AlbumService {


    @Autowired
    private AlbumRepository albumRepository;

    @Autowired
    private IUserRepository userRepository;

    @Autowired
    private SongRepository songRepository;

    @Autowired
    private CloudinaryService cloudinaryService;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private SpringTemplateEngine templateEngine;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserService userService;
    @Autowired
    private SongService songService;

    @Override
    public List<AlbumResponseDTO> getAllAlbums() {
        List<Album> albums = albumRepository.findByStatus(com.ra.base_spring_boot.model.constants.ApprovalStatus.APPROVED);
        return albums.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<AlbumResponseDTO> getAlbumsByArtistId(Long artistId) {
        List<Album> albums = albumRepository.findByUsersId(artistId);
        return albums.stream()
                .filter(album -> album.getStatus() == com.ra.base_spring_boot.model.constants.ApprovalStatus.APPROVED)
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public AlbumResponseDTO createAlbum(AlbumRequestDTO albumRequestDTO, Authentication authentication) {
        Long artistId = getUserIdFromAuthentication(authentication);
        if (artistId == null) {
            throw new HttpUnAuthorized("Access denied. Artist role required.");
        }
        
        var artistOptional = userRepository.findById(artistId);
        if (artistOptional.isEmpty()) {
            throw new HttpBadRequest("Artist not found");
        }

        String coverImageUrl = cloudinaryService.uploadFile(albumRequestDTO.getCoverImageUrl(), "images");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime releaseDate = albumRequestDTO.getReleaseDate() != null
                ? albumRequestDTO.getReleaseDate()
                : now.plusMinutes(5);

        Album album = Album.builder()
                .title(albumRequestDTO.getTitleAlbum())
                .releaseDate(releaseDate)
                .users(artistOptional.get())
                .coverImage(coverImageUrl)
                .type(albumRequestDTO.getType())
                .status(com.ra.base_spring_boot.model.constants.ApprovalStatus.PENDING) // Mặc định là PENDING
                .createdAt(now)
                .updatedAt(now)
                .build();

        Album savedAlbum = albumRepository.save(album);
        return convertToDTO(savedAlbum);
    }

    @Override
    public AlbumResponseDTO updateAlbum(Long id, AlbumRequestDTO albumRequestDTO, Authentication authentication) {
        Album album = albumRepository.findById(id)
                .orElseThrow(() -> new HttpBadRequest("Album not found"));

        Long currentArtistId = getUserIdFromAuthentication(authentication);
        if (!album.getUsers().getId().equals(currentArtistId)) {
            throw new HttpBadRequest("Permission denied. Only album owner can update.");
        }

        var artistOptional = userRepository.findById(currentArtistId);
        if (artistOptional.isEmpty()) {
            throw new HttpBadRequest("Artist not found");
        }

        String coverImageUrl = cloudinaryService.uploadFile(albumRequestDTO.getCoverImageUrl(), "images");

        album.setTitle(albumRequestDTO.getTitleAlbum());
        album.setReleaseDate(
                albumRequestDTO.getReleaseDate() != null ? albumRequestDTO.getReleaseDate() : LocalDateTime.now()
        );
        album.setUsers(artistOptional.get());
        album.setCoverImage(coverImageUrl);
        album.setType(albumRequestDTO.getType());
        album.setUpdatedAt(LocalDateTime.now());

        Album updatedAlbum = albumRepository.save(album);
        return convertToDTO(updatedAlbum);
    }

    @Override
    @Transactional
    public void deleteAlbum(Long id) {
        Album album = albumRepository.findById(id)
                .orElseThrow(() -> new HttpNotFound("Album not found"));

        album.setStatus(com.ra.base_spring_boot.model.constants.ApprovalStatus.DELETED_BY_ARTIST);
        album.setUpdatedAt(LocalDateTime.now());
        albumRepository.save(album);
    }

    @Override
    public List<AlbumResponseDTO> searchAlbumsByTitle(String title) {
        List<Album> albums = albumRepository.findByTitleContainingIgnoreCase(title);
        return albums.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    public Long countAlbum(LocalDateTime start, LocalDateTime end) {
        if(start == null && end == null){
            return albumRepository.count();
        }else if(start.isAfter(end)){
            throw new IllegalArgumentException("start time is after end time");
        }
        return albumRepository.countAlbumByDate(start, end);

    }

    @Override
    public Album getAlbumWithArtist(Long albumId) {
        return albumRepository.findById(albumId)
                .orElseThrow(() -> new HttpNotFound("Album not found"));
    }


    @Override
    public List<AlbumResponseDTO> getPendingAlbums() {
        List<Album> albums = albumRepository.findByStatus(com.ra.base_spring_boot.model.constants.ApprovalStatus.PENDING);
        return albums.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    public List<AlbumResponseDTO> getRejectedAlbums() {
        List<Album> albums = albumRepository.findByStatus(com.ra.base_spring_boot.model.constants.ApprovalStatus.REJECTED);
        return albums.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    public List<AlbumResponseDTO> getDeletedByAdminAlbums() {
        List<Album> albums = albumRepository.findByStatus(com.ra.base_spring_boot.model.constants.ApprovalStatus.DELETED_BY_ADMIN);
        return albums.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void approveAlbum(Long albumId) {
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new HttpNotFound("Album not found"));
        album.setStatus(com.ra.base_spring_boot.model.constants.ApprovalStatus.APPROVED);
        album.setUpdatedAt(LocalDateTime.now());
        albumRepository.save(album);

        // Tạo thông báo cho artist
        notificationService.notifyArtistAlbumApproved(album.getUsers().getId(), album.getTitle());
        // Tạo thông báo cho follower
        List<Long> followerIds = userService.getFollowerIds(userService, album.getUsers().getId());
        notificationService.notifyAlbumApproved(album.getUsers().getId(), album.getTitle(), followerIds);
    }

    @Override
    @Transactional
    public void rejectAlbum(Long albumId, String reason) {
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new HttpNotFound("Album not found"));
        album.setStatus(com.ra.base_spring_boot.model.constants.ApprovalStatus.REJECTED);
        album.setUpdatedAt(LocalDateTime.now());
        albumRepository.save(album);

        try {
            Context context = new Context();
            String artistName = album.getUsers().getFirstName() + " " + album.getUsers().getLastName();
            context.setVariable("artistName", artistName);
            context.setVariable("albumName", album.getTitle());
            context.setVariable("reason", reason);
            context.setVariable("rejectionTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
            context.setVariable("rejectionDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

            String emailContent = templateEngine.process("email/album-rejection-notification", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(album.getUsers().getEmail());
            helper.setSubject("Album Rejection Notification - " + album.getTitle());
            helper.setText(emailContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
        notificationService.notifyAlbumRejected(album.getUsers().getId(), album.getTitle(), reason, null);
    }

    @Override
    @Transactional
    public void restoreAlbum(Long albumId) {
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new HttpNotFound("Album not found"));
        album.setStatus(com.ra.base_spring_boot.model.constants.ApprovalStatus.APPROVED);
        album.setUpdatedAt(LocalDateTime.now());
        albumRepository.save(album);

        try {
            Context context = new Context();
            String artistName = album.getUsers().getFirstName() + " " + album.getUsers().getLastName();
            context.setVariable("artistName", artistName);
            context.setVariable("albumName", album.getTitle());
            context.setVariable("restorationDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            context.setVariable("restorationTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));

            String emailContent = templateEngine.process("email/album-restoration-notification", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(album.getUsers().getEmail());
            helper.setSubject("Album Restoration Notification - " + album.getTitle());
            helper.setText(emailContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<AlbumResponseDTO> getMyRejectedAlbums(Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        if (userId == null) {
            throw new HttpUnAuthorized("Access denied. Artist role required.");
        }
        List<Album> rejectedAlbums = albumRepository.findByUsersIdAndStatus(userId, ApprovalStatus.REJECTED);
        return rejectedAlbums.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<AlbumResponseDTO> getMyDeletedByAdminAlbums(Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        if (userId == null) {
            throw new HttpUnAuthorized("Access denied. Artist role required.");
        }
        List<Album> deletedByAdminAlbums = albumRepository.findByUsersIdAndStatus(userId, ApprovalStatus.DELETED_BY_ADMIN);
        return deletedByAdminAlbums.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<AlbumResponseDTO> getMyPendingAlbums(Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        if (userId == null) {
            throw new HttpUnAuthorized("Access denied. Artist role required.");
        }
        List<Album> pendingAlbums = albumRepository.findByUsersIdAndStatus(userId, ApprovalStatus.PENDING);
        return pendingAlbums.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private Long getUserIdFromAuthentication(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserDetails userDetails) {
                User user = userRepository.findByEmail(userDetails.getUsername()).orElse(null);
                if (user != null) {
                    boolean isArtist = user.getRoles().stream().anyMatch(r ->
                            r.getRoleName() == com.ra.base_spring_boot.model.constants.RoleName.ROLE_ARTIST ||
                            r.getRoleName() == com.ra.base_spring_boot.model.constants.RoleName.ROLE_ADMIN ||
                            r.getRoleName().name().equals("ARTIST") ||
                            r.getRoleName().name().equals("ADMIN")
                    );
                    if (isArtist) {
                        return user.getId();
                    }
                }
            } else if (principal instanceof User user) {
                boolean isArtist = user.getRoles().stream().anyMatch(r ->
                        r.getRoleName() == com.ra.base_spring_boot.model.constants.RoleName.ROLE_ARTIST ||
                        r.getRoleName() == com.ra.base_spring_boot.model.constants.RoleName.ROLE_ADMIN ||
                        r.getRoleName().name().equals("ARTIST") ||
                        r.getRoleName().name().equals("ADMIN")
                );
                if (isArtist) {
                    return user.getId();
                }
            }
        }
        return null;
    }

    private AlbumResponseDTO convertToDTO(Album album) {
        Integer artistId = album.getUsers().getId() != null ? album.getUsers().getId().intValue() : null;
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        String releaseDateStr = album.getReleaseDate() != null ? album.getReleaseDate().format(formatter) : null;
        String createdAtStr = album.getCreatedAt() != null ? album.getCreatedAt().format(formatter) : null;
        String updatedAtStr = album.getUpdatedAt() != null ? album.getUpdatedAt().format(formatter) : null;
        return AlbumResponseDTO.builder()
                .id(album.getId())
                .titleAlbum(album.getTitle())
                .releaseDateStr(releaseDateStr)
                .artistId(artistId)
                .coverImageUrl(album.getCoverImage())
                .type(album.getType())
                .status(album.getStatus() != null ? album.getStatus().name() : null)
                .createdAtStr(createdAtStr)
                .updatedAtStr(updatedAtStr)
                .build();
    }

    @Override
    public void deleteAlbumByAdmin(Long albumId, String reason) {
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new RuntimeException("Album not found"));

        album.setStatus(com.ra.base_spring_boot.model.constants.ApprovalStatus.DELETED_BY_ADMIN);
        album.setUpdatedAt(LocalDateTime.now());

        albumRepository.save(album);

        try {
            Context context = new Context();
            String artistName = album.getUsers().getFirstName() + " " + album.getUsers().getLastName();
            context.setVariable("artistName", artistName);
            context.setVariable("albumName", album.getTitle());
            context.setVariable("reason", reason);
            context.setVariable("deletionTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
            context.setVariable("deletionDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

            String emailContent = templateEngine.process("email/album-deletion-notification", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(album.getUsers().getEmail());
            helper.setSubject("Album Deletion Notification - " + album.getTitle());
            helper.setText(emailContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }


    @Override
    public List<AlbumResponseDTO> getMyDeletedAlbums(Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        if (userId == null) {
            throw new HttpUnAuthorized("Access denied. Artist role required.");
        }

        List<Album> deletedAlbums = albumRepository.findByUsersIdAndStatus(userId,
            com.ra.base_spring_boot.model.constants.ApprovalStatus.DELETED_BY_ARTIST);
        return deletedAlbums.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void requestRestoreAlbum(Long albumId) {
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new HttpNotFound("Album not found"));

        if (album.getStatus() != com.ra.base_spring_boot.model.constants.ApprovalStatus.REJECTED &&
            album.getStatus() != com.ra.base_spring_boot.model.constants.ApprovalStatus.DELETED_BY_ADMIN &&
            album.getStatus() != com.ra.base_spring_boot.model.constants.ApprovalStatus.DELETED_BY_ARTIST) {
            throw new HttpNotFound("Only albums with Inappropriate Content or Deleted status can be requested for restoration");
        }

        album.setStatus(com.ra.base_spring_boot.model.constants.ApprovalStatus.PENDING);
        album.setUpdatedAt(LocalDateTime.now());
        albumRepository.save(album);
    }
    @Override
    public List<AlbumResponseDTO> fullTextSearchAlbums(String keyword) {
        List<Album> albumList = albumRepository.fullTextSearchAlbum(keyword);
        return albumList.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    public AlbumDetailResponseDTO getAlbumDetailWithOwner(Long albumId) {
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new HttpNotFound("Album not found"));

        User artist = album.getUsers();
        if (artist == null) {
            throw new HttpNotFound("Album owner not found");
        }

        AlbumOwnerInfo ownerInfo = buildAlbumOwnerInfo(artist);

        AlbumStats stats = buildAlbumStats(albumId);

        return AlbumDetailResponseDTO.builder()
                .albumId(album.getId())
                .title(album.getTitle())
                .releaseDate(album.getReleaseDate())
                .coverImage(album.getCoverImage())
                .type(album.getType())
                .status(album.getStatus())
                .createdAt(album.getCreatedAt())
                .updatedAt(album.getUpdatedAt())
                .owner(ownerInfo)
                .stats(stats)
                .build();
    }

    private AlbumOwnerInfo buildAlbumOwnerInfo(User artist) {
        // Tính toán thống kê của artist
        Integer totalAlbums = albumRepository.findByUsersId(artist.getId()).size();
        Integer totalSongs = getTotalSongsByArtist(artist.getId());
        Integer totalFollowers = getTotalFollowersByArtist(artist.getId());

        return AlbumOwnerInfo.builder()
                .userId(artist.getId())
                .fullName(artist.getFirstName() + " " + artist.getLastName())
                .firstName(artist.getFirstName())
                .lastName(artist.getLastName())
                .email(artist.getEmail())
                .profileImage(artist.getProfileImage())
                .bio(artist.getBio())
                .age(artist.getAge())
                .joinedDate(artist.getCreatedAt())
                .totalAlbums(totalAlbums)
                .totalSongs(totalSongs)
                .totalFollowers(totalFollowers)
                .build();
    }

    private AlbumStats buildAlbumStats(Long albumId) {
        // Tính toán thống kê album
        Integer totalSongs = getTotalSongsByAlbum(albumId);
        Long totalViews = getTotalViewsByAlbum(albumId);
        Integer totalDownloads = getTotalDownloadsByAlbum(albumId);
        String totalDuration = getTotalDurationByAlbum(albumId);

        return AlbumStats.builder()
                .totalSongs(totalSongs)
                .totalViews(totalViews)
                .totalDownloads(totalDownloads)
                .totalDuration(totalDuration)
                .build();
    }

    private Integer getTotalSongsByArtist(Long artistId) {
        return songRepository.countByUsersIdAndStatus(artistId, ApprovalStatus.APPROVED);
    }

    private Integer getTotalFollowersByArtist(Long artistId) {
        User artist = userRepository.findById(artistId).orElse(null);
        return artist != null ? artist.getFollowers().size() : 0;
    }

    private Integer getTotalSongsByAlbum(Long albumId) {
        return songRepository.countByAlbumsId(albumId);
    }

    private Long getTotalViewsByAlbum(Long albumId) {
        return songRepository.sumViewsByAlbumId(albumId);
    }

    private Integer getTotalDownloadsByAlbum(Long albumId) {
        return songRepository.sumDownloadsByAlbumId(albumId);
    }

    private String getTotalDurationByAlbum(Long albumId) {
        List<String> durations = songRepository.findDurationsByAlbumId(albumId);

        if (durations.isEmpty()) {
            return "00:00:00";
        }

        int totalSeconds = 0;
        for (String duration : durations) {
            if (duration != null && !duration.isEmpty()) {
                totalSeconds += convertDurationToSeconds(duration);
            }
        }

        return convertSecondsToFormat(totalSeconds);
    }

    private int convertDurationToSeconds(String duration) {
        try {
            String[] parts = duration.split(":");
            int hours = 0, minutes = 0, seconds = 0;

            if (parts.length == 3) {
                hours = Integer.parseInt(parts[0]);
                minutes = Integer.parseInt(parts[1]);
                seconds = Integer.parseInt(parts[2]);
            } else if (parts.length == 2) {
                minutes = Integer.parseInt(parts[0]);
                seconds = Integer.parseInt(parts[1]);
            }

            return hours * 3600 + minutes * 60 + seconds;
        } catch (Exception e) {
            return 0;
        }
    }

    private String convertSecondsToFormat(int totalSeconds) {
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
    @Override
    public AlbumInfoResponseDTO getAlbumInfo(Long albumId) {
        AlbumInfoResponseDTO albumInfoResponseDTO=new AlbumInfoResponseDTO();
        if (!albumRepository.existsById(albumId)) {
            throw new HttpBadRequest("Album not found");
        }
        albumInfoResponseDTO.setAlbumDetailResponseDTO(getAlbumDetailWithOwner(albumId));
        albumInfoResponseDTO.setSongResponseDTO(songService.getApprovedSongsByAlbum(albumId));
        return albumInfoResponseDTO;
    }


}
