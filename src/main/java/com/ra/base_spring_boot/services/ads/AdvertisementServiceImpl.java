package com.ra.base_spring_boot.services.ads;

import com.ra.base_spring_boot.dto.req.AdvertisementRequest;
import com.ra.base_spring_boot.dto.resp.AdvertisementResponse;
import com.ra.base_spring_boot.model.Advertisement;
import com.ra.base_spring_boot.model.User;
import com.ra.base_spring_boot.model.constants.AdType;
import com.ra.base_spring_boot.model.constants.ApprovalStatus;
import com.ra.base_spring_boot.repository.AdvertisementRepository;
import com.ra.base_spring_boot.repository.IUserRepository;
import com.ra.base_spring_boot.services.impl.CloudinaryService;
import com.ra.base_spring_boot.dto.req.NotificationRequest;
import com.ra.base_spring_boot.services.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdvertisementServiceImpl implements AdvertisementService {
    private final AdvertisementRepository advertisementRepository;
    private final IUserRepository userRepository;
    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final CloudinaryService cloudinaryService;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public AdvertisementResponse createAdvertisement(AdvertisementRequest request, String creatorEmail) {
        User creator = userRepository.findByEmail(creatorEmail)
                .orElseThrow(() -> new RuntimeException("No user found with email: " + creatorEmail));

        boolean isAdmin = creator.getRoles().stream()
                .anyMatch(role -> role.getRoleName() == com.ra.base_spring_boot.model.constants.RoleName.ROLE_ADMIN);

        if (!isAdmin) {
            long activeAds = advertisementRepository.findAll().stream()
                    .filter(ad -> ad.getCreatedBy() != null && ad.getCreatedBy().getId().equals(creator.getId()))
                    .filter(ad -> ad.getStatus() == ApprovalStatus.PENDING || ad.getStatus() == ApprovalStatus.APPROVED)
                    .count();
            if (activeAds >= 4) {
                throw new RuntimeException("You can only have a maximum of 4 active or pending ads!");
            }
        }

        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(30);
        String mediaUrl = saveMediaFile(request.getMediaFile());

        Advertisement ad = Advertisement.builder()
                .adType(AdType.valueOf(request.getAdType().toUpperCase()))
                .status(isAdmin ? ApprovalStatus.APPROVED : ApprovalStatus.PENDING)
                .startDate(startDate)
                .endDate(endDate)
                .mediaUrl(mediaUrl)
                .redirectUrl(request.getRedirectUrl())
                .createdBy(creator)
                .build();
        ad = advertisementRepository.save(ad);
        return convertToDTO(ad);
    }

    @Override
    public List<AdvertisementResponse> getAllAdvertisements() {
        List<Advertisement> ads = advertisementRepository.findAll();
        return ads.stream()
                .filter(ad -> ad.getStatus() != ApprovalStatus.EXPIRED)
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<AdvertisementResponse> getMyApprovedAdvertisements() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new RuntimeException("User not authenticated");
        }

        String currentUserEmail = authentication.getName();
        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new RuntimeException("No user found with email: " + currentUserEmail));

        List<Advertisement> ads = advertisementRepository.findAll().stream()
                .filter(ad -> ad.getCreatedBy() != null && ad.getCreatedBy().equals(currentUser))
                .filter(ad -> ad.getStatus() == ApprovalStatus.APPROVED || ad.getStatus() == ApprovalStatus.PENDING)
                .collect(Collectors.toList());

        return ads.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AdvertisementResponse approveAdvertisement(Long adId) {
        Advertisement ad = advertisementRepository.findById(adId)
                .orElseThrow(() -> new RuntimeException("Ad does not exist!"));
        ad.setStatus(ApprovalStatus.APPROVED);
        advertisementRepository.save(ad);
        // Gửi email cho artist (dùng template)
        try {
            Context context = new Context();
            String artistName = ad.getCreatedBy().getFirstName() + " " + ad.getCreatedBy().getLastName();
            context.setVariable("artistName", artistName);
            context.setVariable("adTitle", "Advertisement");
            context.setVariable("approvalTime", java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
            String emailContent = templateEngine.process("email/advertisement-approval-notification", context);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(ad.getCreatedBy().getEmail());
            helper.setSubject("Advertisement Approval Notification");
            helper.setText(emailContent, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Cannot send approval email", e);
        }
        return convertToDTO(ad);
    }

    @Override
    @Transactional
    public AdvertisementResponse rejectAdvertisement(Long adId, String reason) {
        Advertisement ad = advertisementRepository.findById(adId)
                .orElseThrow(() -> new RuntimeException("Ad does not exist!"));
        ad.setStatus(ApprovalStatus.REJECTED);
        ad.setRejectReason(reason);
        advertisementRepository.save(ad);
        // Gửi email từ chối cho artist (dùng template)
        try {
            Context context = new Context();
            String artistName = ad.getCreatedBy().getFirstName() + " " + ad.getCreatedBy().getLastName();
            context.setVariable("artistName", artistName);
            context.setVariable("adTitle", "Advertisement");
            context.setVariable("reason", reason);
            context.setVariable("rejectionTime", java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
            String emailContent = templateEngine.process("email/advertisement-rejection-notification", context);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(ad.getCreatedBy().getEmail());
            helper.setSubject("Advertisement Rejection Notification");
            helper.setText(emailContent, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Cannot send rejection email", e);
        }
        return convertToDTO(ad);
    }

    @Override
    public List<AdvertisementResponse> getPendingAdvertisements() {
        List<Advertisement> ads = advertisementRepository.findByStatus(ApprovalStatus.PENDING);
        return ads.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    public List<AdvertisementResponse> getRejectedAdvertisements() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new RuntimeException("User not authenticated");
        }

        String currentUserEmail = authentication.getName();
        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new RuntimeException("No user found with email: " + currentUserEmail));

        List<Advertisement> ads = advertisementRepository.findByStatus(ApprovalStatus.REJECTED)
                .stream()
                .filter(ad -> ad.getCreatedBy() != null && ad.getCreatedBy().equals(currentUser))
                .collect(Collectors.toList());

        return ads.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    public List<AdvertisementResponse> getApprovedBannerList() {
        return advertisementRepository.findByStatus(com.ra.base_spring_boot.model.constants.ApprovalStatus.APPROVED)
                .stream()
                .filter(ad -> ad.getAdType() == com.ra.base_spring_boot.model.constants.AdType.BANNER)
                .map(this::convertToDTO)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public AdvertisementResponse getRandomApprovedAudioAd() {
        List<Advertisement> ads = advertisementRepository.findByStatus(com.ra.base_spring_boot.model.constants.ApprovalStatus.APPROVED)
                .stream()
                .filter(ad -> ad.getAdType() == com.ra.base_spring_boot.model.constants.AdType.AUDIO)
                .toList();
        if (ads.isEmpty()) return null;
        Advertisement ad = ads.get((int) (Math.random() * ads.size()));
        // Lấy user hiện tại
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isPremium = false;
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            isPremium = authentication.getAuthorities().stream()
                    .anyMatch(r -> r.getAuthority().equals("ROLE_PREMIUM"));
        }
        if (!isPremium) {
            return convertToDTO(ad);
        } else {
            return null;
        }
    }

    @Override
    @Transactional
    public int deleteExpiredAdvertisements() {
        List<Advertisement> expiredAds = advertisementRepository.findByEndDateBefore(LocalDate.now())
                .stream()
                .filter(ad -> ad.getStatus() != ApprovalStatus.EXPIRED)
                .collect(Collectors.toList());
        expiredAds.forEach(ad -> {
            ad.setStatus(ApprovalStatus.EXPIRED);
            if (ad.getCreatedBy() != null) {
                String message = "Your ad has expired, please renew if needed";
                notificationService.createNotification(
                        new NotificationRequest(
                                ad.getCreatedBy().getId(),
                                message,
                                "advertisement_expired"
                        )
                );
            }
        });
        advertisementRepository.saveAll(expiredAds);
        return expiredAds.size();
    }

    @Override
    @Transactional
    public AdvertisementResponse renewAdvertisement(Long adId, String startDateStr, String endDateStr, String artistEmail) {
        Advertisement ad = advertisementRepository.findById(adId)
                .orElseThrow(() -> new RuntimeException("No ad found!"));
        if (ad.getStatus() != ApprovalStatus.EXPIRED) {
            throw new RuntimeException("Only expired ads can be renewed!");
        }
        if (ad.getCreatedBy() == null || !ad.getCreatedBy().getEmail().equals(artistEmail)) {
            throw new RuntimeException("You do not have permission to renew this ad!");
        }
        // Đếm số quảng cáo chưa hết hạn hoặc đang chờ duyệt/đã duyệt/đang chạy của user (không tính cái đang renew)
        long activeAds = advertisementRepository.findAll().stream()
                .filter(a -> a.getCreatedBy() != null && a.getCreatedBy().getId().equals(ad.getCreatedBy().getId()))
                .filter(a -> a.getStatus() == ApprovalStatus.PENDING || a.getStatus() == ApprovalStatus.APPROVED)
                .filter(a -> !a.getId().equals(adId))
                .count();
        if (activeAds >= 4) {
            throw new RuntimeException("You can only have a maximum of 4 active or pending ads!");
        }
        try {
            LocalDate newStart = LocalDate.parse(startDateStr);
            LocalDate newEnd = LocalDate.parse(endDateStr);
            if (newEnd.isBefore(newStart)) {
                throw new RuntimeException("End date must be after start date!");
            }
            ad.setStartDate(newStart);
            ad.setEndDate(newEnd);
            ad.setStatus(ApprovalStatus.PENDING);
            ad.setRejectReason(null);
            advertisementRepository.save(ad);
            return convertToDTO(ad);
        } catch (DateTimeParseException e) {
            throw new RuntimeException("Invalid date format! (yyyy-MM-dd)");
        }
    }

    @Override
    public List<AdvertisementResponse> getArtistApprovedAdvertisements(String artistEmail) {
        User artist = userRepository.findByEmail(artistEmail)
                .orElseThrow(() -> new RuntimeException("No user found with email: " + artistEmail));
        List<Advertisement> ads = advertisementRepository.findAll().stream()
                .filter(ad -> ad.getCreatedBy() != null && ad.getCreatedBy().equals(artist) && ad.getStatus() == ApprovalStatus.APPROVED)
                .collect(Collectors.toList());
        return ads.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    public List<AdvertisementResponse> getAllApprovedAdvertisements() {
        List<Advertisement> ads = advertisementRepository.findByStatus(ApprovalStatus.APPROVED);
        return ads.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteMyAdvertisement(Long adId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new RuntimeException("User not authenticated");
        }
        String currentUserEmail = authentication.getName();
        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new RuntimeException("No user found with email: " + currentUserEmail));
        Advertisement ad = advertisementRepository.findById(adId)
                .orElseThrow(() -> new RuntimeException("Ad does not exist!"));
        if (ad.getCreatedBy() == null || !ad.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You do not have permission to delete this advertisement");
        }
        advertisementRepository.delete(ad);
    }

    private String saveMediaFile(MultipartFile file) {
        if (file == null || file.isEmpty()) return null;
        return cloudinaryService.uploadFile(file, "advertisements");
    }

    private AdvertisementResponse convertToDTO(Advertisement ad) {
        AdvertisementResponse response = new AdvertisementResponse();
        response.setId(ad.getId());
        response.setAdType(ad.getAdType().name());
        response.setStatus(ad.getStatus().name());
        response.setMediaUrl(ad.getMediaUrl());
        response.setRedirectUrl(ad.getRedirectUrl());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        response.setStartDate(ad.getStartDate());
        response.setEndDate(ad.getEndDate());
        response.setStartDateStr(ad.getStartDate() != null ? ad.getStartDate().atStartOfDay().format(formatter) : null);
        response.setEndDateStr(ad.getEndDate() != null ? ad.getEndDate().atStartOfDay().format(formatter) : null);
        if (ad.getCreatedBy() != null) {
            response.setCreatedById(ad.getCreatedBy().getId());
            response.setCreatedByEmail(ad.getCreatedBy().getEmail());
            response.setCreatedByName(ad.getCreatedBy().getFirstName() + " " + ad.getCreatedBy().getLastName());
        }
        response.setRejectReason(ad.getRejectReason());
        return response;
    }
}
