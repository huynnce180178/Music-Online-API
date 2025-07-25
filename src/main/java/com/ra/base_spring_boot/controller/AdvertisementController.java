package com.ra.base_spring_boot.controller;

import com.ra.base_spring_boot.dto.ResponseWrapper;
import com.ra.base_spring_boot.dto.req.AdvertisementRequest;
import com.ra.base_spring_boot.dto.resp.AdvertisementResponse;
import com.ra.base_spring_boot.services.ads.AdvertisementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;

@Tag(name="Advertisement Controller")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AdvertisementController {
    private final AdvertisementService advertisementService;

    @Operation(summary = "Duyệt quảng cáo", description = "Admin duyệt quảng cáo do artist gửi lên")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Advertisement approved successfully"),
        @ApiResponse(responseCode = "404", description = "Advertisement not found")
    })
    @PostMapping("/admin/advertisements/approve/{id}")
    public ResponseEntity<ResponseWrapper<AdvertisementResponse>> approveAd(@PathVariable Long id) {
        try {
            AdvertisementResponse approved = advertisementService.approveAdvertisement(id);
            return ResponseEntity.ok(
                ResponseWrapper.<AdvertisementResponse>builder()
                    .data(approved)
                    .code(HttpStatus.OK.value())
                    .status(HttpStatus.OK)
                    .build()
            );
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ResponseWrapper.<AdvertisementResponse>builder()
                    .message(ex.getMessage())
                    .code(HttpStatus.NOT_FOUND.value())
                    .status(HttpStatus.NOT_FOUND)
                    .build()
            );
        }
    }

    @Operation(summary = "Từ chối quảng cáo", description = "Admin từ chối quảng cáo do artist gửi lên")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Advertisement rejected successfully"),
        @ApiResponse(responseCode = "404", description = "Advertisement not found")
    })
    @PostMapping("/admin/advertisements/reject/{id}")
    public ResponseEntity<ResponseWrapper<AdvertisementResponse>> rejectAd(@PathVariable Long id, @RequestParam String reason) {
        try {
            AdvertisementResponse rejected = advertisementService.rejectAdvertisement(id, reason);
            return ResponseEntity.ok(
                ResponseWrapper.<AdvertisementResponse>builder()
                    .data(rejected)
                    .code(HttpStatus.OK.value())
                    .status(HttpStatus.OK)
                    .build()
            );
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ResponseWrapper.<AdvertisementResponse>builder()
                        .message(ex.getMessage() != null ? ex.getMessage() : "Ad does not exist!")                    .code(HttpStatus.NOT_FOUND.value())
                    .status(HttpStatus.NOT_FOUND)
                    .build()
            );
        }
    }

    @Operation(summary = "Lấy danh sách quảng cáo của artist", description = "Artist lấy danh sách quảng cáo của chính mình đã được duyệt")
    @GetMapping("/artist/advertisements")
    public ResponseEntity<ResponseWrapper<List<AdvertisementResponse>>> getArtistApprovedAds(Principal principal) {
        List<AdvertisementResponse> ads = advertisementService.getArtistApprovedAdvertisements(principal.getName());
        if (ads == null || ads.isEmpty()) {
            return ResponseEntity.ok(
                ResponseWrapper.<List<AdvertisementResponse>>builder()
                    .message("No approved ads for this artist yet")
                    .code(HttpStatus.OK.value())
                    .status(HttpStatus.OK)
                    .build()
            );
        }
        return ResponseEntity.ok(
            ResponseWrapper.<List<AdvertisementResponse>>builder()
                .data(ads)
                .code(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build()
        );
    }

    @Operation(summary = "Lấy danh sách quảng cáo đã duyệt", description = "Admin lấy danh sách tất cả quảng cáo đã duyệt")
    @GetMapping("/admin/advertisements")
    public ResponseEntity<ResponseWrapper<List<AdvertisementResponse>>> getAllApprovedAds() {
        List<AdvertisementResponse> ads = advertisementService.getAllApprovedAdvertisements();
        if (ads == null || ads.isEmpty()) {
            return ResponseEntity.ok(
                ResponseWrapper.<List<AdvertisementResponse>>builder()
                    .message("No approved ads yet")
                    .code(HttpStatus.OK.value())
                    .status(HttpStatus.OK)
                    .build()
            );
        }
        return ResponseEntity.ok(
            ResponseWrapper.<List<AdvertisementResponse>>builder()
                .data(ads)
                .code(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build()
        );
    }

    @Operation(summary = "Artist gửi yêu cầu quảng cáo", description = "Artist tạo mới quảng cáo banner/audio")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Advertisement created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    @PostMapping("/artist/advertisements/create")
    public ResponseEntity<ResponseWrapper<AdvertisementResponse>> createAd(
            @RequestParam("adType") String adType,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate,
            @RequestParam("redirectUrl") String redirectUrl,
            @RequestParam(value = "mediaFile", required = false) MultipartFile mediaFile,
            Principal principal) {
        try {
            // Tạo AdvertisementRequest object thủ công
            AdvertisementRequest request = new AdvertisementRequest();
            request.setAdType(adType);
            request.setStartDate(java.time.LocalDate.parse(startDate));
            request.setEndDate(java.time.LocalDate.parse(endDate));
            request.setRedirectUrl(redirectUrl);
            request.setMediaFile(mediaFile);

            AdvertisementResponse created = advertisementService.createAdvertisement(request, principal.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(
                ResponseWrapper.<AdvertisementResponse>builder()
                    .data(created)
                    .code(HttpStatus.CREATED.value())
                    .status(HttpStatus.CREATED)
                    .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ResponseWrapper.<AdvertisementResponse>builder()
                    .message(e.getMessage())
                    .code(HttpStatus.BAD_REQUEST.value())
                    .status(HttpStatus.BAD_REQUEST)
                    .build()
            );
        }
    }

    @Operation(summary = "Lấy danh sách quảng cáo chờ duyệt", description = "Admin lấy danh sách tất cả quảng cáo chờ duyệt")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved pending advertisements")
    })
    @GetMapping("/admin/advertisements/pending")
    public ResponseEntity<List<AdvertisementResponse>> getPendingAdvertisements() {
        return ResponseEntity.ok(advertisementService.getPendingAdvertisements());
    }

    @Operation(summary = "Lấy danh sách quảng cáo bị từ chối", description = "Admin lấy danh sách tất cả quảng cáo bị từ chối")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved rejected advertisements")
    })
    @GetMapping("/artist/advertisements/rejected")
    public ResponseEntity<List<AdvertisementResponse>> getRejectedAdvertisements() {
        return ResponseEntity.ok(advertisementService.getRejectedAdvertisements());
    }


    @Operation(summary = "Lấy danh sách banner đã duyệt", description = "Lấy tất cả quảng cáo banner đã duyệt")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved approved banners"),
        @ApiResponse(responseCode = "204", description = "No approved banners available")
    })
    @GetMapping("/guest/banner/list")
    public ResponseEntity<ResponseWrapper<List<AdvertisementResponse>>> getApprovedBannerList() {
        List<AdvertisementResponse> approvedBanners = advertisementService.getApprovedBannerList();
        if (approvedBanners.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body(
                ResponseWrapper.<List<AdvertisementResponse>>builder()
                    .message("No banners approved!")
                    .code(HttpStatus.NO_CONTENT.value())
                    .status(HttpStatus.NO_CONTENT)
                    .build()
            );
        }
        return ResponseEntity.ok(
            ResponseWrapper.<List<AdvertisementResponse>>builder()
                .data(approvedBanners)
                .code(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build()
        );
    }


    @Operation(summary = "Phát quảng cáo audio ngẫu nhiên", description = "Phát 1 quảng cáo audio đã duyệt như 1 bài hát (play ad audio)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully played random audio ad"),
        @ApiResponse(responseCode = "204", description = "No audio ad available")
    })
    @GetMapping("/guest/audio/play")
    public ResponseEntity<ResponseWrapper<?>> playRandomAudioAd(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            boolean isPremium = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(role -> role.equals("ROLE_PREMIUM"));
            if (isPremium) {
                return ResponseEntity.ok(
                    ResponseWrapper.builder()
                        .data(null)
                        .message("No ads for premium user.")
                        .code(HttpStatus.OK.value())
                        .status(HttpStatus.OK)
                        .build()
                );
            }
        }
        AdvertisementResponse ad = advertisementService.getRandomApprovedAudioAd();
        if (ad == null) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body(
                ResponseWrapper.builder()
                    .message("No audio ads approved!")
                    .code(HttpStatus.NO_CONTENT.value())
                    .status(HttpStatus.NO_CONTENT)
                    .build()
            );
        }
        return ResponseEntity.ok(
            ResponseWrapper.builder()
                .data(ad)
                .code(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build()
        );
    }

    @Operation(summary = "Xóa quảng cáo hết hạn", description = "Admin xóa tất cả quảng cáo đã hết hạn (endDate < ngày hiện tại)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully deleted expired advertisements")
    })
    @DeleteMapping("/admin/advertisements/expired")
    public ResponseEntity<ResponseWrapper<Integer>> deleteExpiredAdvertisements() {
        int deletedCount = advertisementService.deleteExpiredAdvertisements();
        return ResponseEntity.ok(
            ResponseWrapper.<Integer>builder()
                .data(deletedCount)
                .message("Deleted " + deletedCount + " expired ad.")
                .code(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build()
        );
    }

    @Operation(summary = "Gia hạn quảng cáo", description = "Nghệ sĩ gia hạn quảng cáo đã hết hạn (chuyển trạng thái EXPIRED về PENDING và cập nhật ngày)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Gia hạn quảng cáo thành công"),
        @ApiResponse(responseCode = "400", description = "Không thể gia hạn quảng cáo")
    })
    @PostMapping("/artist/advertisements/renew/{id}")
    public ResponseEntity<ResponseWrapper<AdvertisementResponse>> renewAdvertisement(
            @PathVariable Long id,
            @RequestParam("startDate") String startDateStr,
            @RequestParam("endDate") String endDateStr,
            Principal principal) {
        try {
            AdvertisementResponse renewed = advertisementService.renewAdvertisement(id, startDateStr, endDateStr, principal.getName());
            return ResponseEntity.ok(
                ResponseWrapper.<AdvertisementResponse>builder()
                    .data(renewed)
                    .code(HttpStatus.OK.value())
                    .status(HttpStatus.OK)
                    .message("Ad renewal successful!")
                    .build()
            );
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ResponseWrapper.<AdvertisementResponse>builder()
                    .message(ex.getMessage())
                    .code(HttpStatus.BAD_REQUEST.value())
                    .status(HttpStatus.BAD_REQUEST)
                    .build()
            );
        }
    }

    @Operation(summary = "Lấy danh sách quảng cáo đã duyệt của tôi", description = "Artist lấy danh sách quảng cáo đã duyệt của chính mình")
    @GetMapping("/artist/advertisements/approved")
    public ResponseEntity<ResponseWrapper<List<AdvertisementResponse>>> getMyApprovedAdvertisements() {
        try {
            List<AdvertisementResponse> ads = advertisementService.getMyApprovedAdvertisements();
            return ResponseEntity.ok(ResponseWrapper.<List<AdvertisementResponse>>builder()
                    .data(ads)
                    .code(HttpStatus.OK.value())
                    .status(HttpStatus.OK)
                    .message(ads.isEmpty() ? "No approved ads for this artist yet" : null)
                    .build());
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    ResponseWrapper.<List<AdvertisementResponse>>builder()
                            .message(ex.getMessage())
                            .code(HttpStatus.UNAUTHORIZED.value())
                            .status(HttpStatus.UNAUTHORIZED)
                            .build());
        }
    }

    @Operation(summary = "Admin thêm quảng cáo", description = "Admin tạo mới quảng cáo banner/audio không giới hạn số lượng")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Advertisement created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    @PostMapping("/admin/advertisements/create")
    public ResponseEntity<ResponseWrapper<AdvertisementResponse>> createAdByAdmin(
            @RequestParam("adType") String adType,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate,
            @RequestParam("redirectUrl") String redirectUrl,
            @RequestParam(value = "mediaFile", required = false) MultipartFile mediaFile,
            Authentication authentication) {
        try {
            AdvertisementRequest request = new AdvertisementRequest();
            request.setAdType(adType);
            request.setStartDate(java.time.LocalDate.parse(startDate));
            request.setEndDate(java.time.LocalDate.parse(endDate));
            request.setRedirectUrl(redirectUrl);
            request.setMediaFile(mediaFile);
            String adminEmail = authentication.getName();
            AdvertisementResponse created = advertisementService.createAdvertisement(request, adminEmail);
            return ResponseEntity.status(HttpStatus.CREATED).body(
                ResponseWrapper.<AdvertisementResponse>builder()
                    .data(created)
                    .code(HttpStatus.CREATED.value())
                    .status(HttpStatus.CREATED)
                    .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ResponseWrapper.<AdvertisementResponse>builder()
                    .message(e.getMessage())
                    .code(HttpStatus.BAD_REQUEST.value())
                    .status(HttpStatus.BAD_REQUEST)
                    .build()
            );
        }
    }
}
