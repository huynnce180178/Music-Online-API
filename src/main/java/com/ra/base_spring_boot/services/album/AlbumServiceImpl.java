package com.ra.base_spring_boot.services.album;

import com.ra.base_spring_boot.controller.FollowController;
import com.ra.base_spring_boot.dto.req.AlbumRequestDTO;
import com.ra.base_spring_boot.dto.req.NotificationRequest;
import com.ra.base_spring_boot.dto.resp.AlbumResponseDTO;
import com.ra.base_spring_boot.exception.HttpNotFound;
import com.ra.base_spring_boot.exception.HttpUnAuthorized;
import com.ra.base_spring_boot.model.Album;
import com.ra.base_spring_boot.model.User;
import com.ra.base_spring_boot.model.constants.ApprovalStatus;
import com.ra.base_spring_boot.repository.AlbumRepository;
import com.ra.base_spring_boot.repository.IUserRepository;
import com.ra.base_spring_boot.repository.SongRepository;
import com.ra.base_spring_boot.services.impl.CloudinaryService;
import com.ra.base_spring_boot.services.notification.NotificationService;
import com.ra.base_spring_boot.services.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private CloudinaryService cloudinaryService;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private SpringTemplateEngine templateEngine;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserService userService;

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
    public AlbumResponseDTO createAlbum(AlbumRequestDTO albumRequestDTO) {
        Long artistId = Long.valueOf(albumRequestDTO.getArtistId());
        var artistOptional = userRepository.findById(artistId);

        if (artistOptional.isEmpty()) {
            throw new RuntimeException("Artist not found");
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
    public AlbumResponseDTO updateAlbum(Long id, AlbumRequestDTO albumRequestDTO) {
        var albumOptional = albumRepository.findById(id);
        if (albumOptional.isPresent()) {
            Album album = albumOptional.get();

            var artistOptional = userRepository.findById(Long.valueOf(albumRequestDTO.getArtistId()));
            if (artistOptional.isEmpty()) {
                throw new RuntimeException("Artist not found");
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
        return null;
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
    public Page<AlbumResponseDTO> getPendingAlbumsPaged(Pageable pageable) {
        Page<Album> albumPage = albumRepository.findByStatusPaged(
                com.ra.base_spring_boot.model.constants.ApprovalStatus.PENDING,
                pageable);
        return albumPage.map(this::convertToDTO);
    }

    @Override
    public Page<AlbumResponseDTO> getRejectedAlbumsPaged(Pageable pageable) {
        Page<Album> albumPage = albumRepository.findByStatusPaged(
                com.ra.base_spring_boot.model.constants.ApprovalStatus.REJECTED,
                pageable);
        return albumPage.map(this::convertToDTO);
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
            throw new HttpUnAuthorized("You do not have permission to view your rejected albums (ROLE_ARTIST required)");
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
            throw new HttpUnAuthorized("You do not have permission to view your albums deleted by admin (ROLE_ARTIST required)");
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
            throw new HttpUnAuthorized("You do not have permission to view your pending albums (ROLE_ARTIST required)");
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
    public Page<AlbumResponseDTO> getMyRejectedAlbumsPaged(Authentication authentication, Pageable pageable) {
        Long userId = getUserIdFromAuthentication(authentication);
        if (userId == null) {
            throw new HttpUnAuthorized("You do not have permission to view your rejected albums (ROLE_ARTIST required)");
        }

        Page<Album> albumPage = albumRepository.findByUsersIdAndStatusPaged(
                userId,
                com.ra.base_spring_boot.model.constants.ApprovalStatus.REJECTED,
                pageable);
        return albumPage.map(this::convertToDTO);
    }

    @Override
    public List<AlbumResponseDTO> getMyDeletedAlbums(Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        if (userId == null) {
            throw new HttpUnAuthorized("You do not have permission to view your deleted albums (ROLE_ARTIST required)");
        }

        List<Album> deletedAlbums = albumRepository.findByUsersIdAndStatus(userId,
            com.ra.base_spring_boot.model.constants.ApprovalStatus.DELETED_BY_ARTIST);
        return deletedAlbums.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Page<AlbumResponseDTO> getDeletedByAdminAlbumsPaged(Pageable pageable) {
        Page<Album> albumPage = albumRepository.findByStatusPaged(
                com.ra.base_spring_boot.model.constants.ApprovalStatus.DELETED_BY_ADMIN,
                pageable);
        return albumPage.map(this::convertToDTO);
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

    // Hàm giả lập lấy danh sách followerIds, bạn cần thay bằng logic thực tế
    private List<Long> getFollowerIdsByArtistId(Long artistId) {
        // TODO: Lấy danh sách followerIds từ repository hoặc service
        return new java.util.ArrayList<>();
    }
}
