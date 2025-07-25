package com.ra.base_spring_boot.controller;

import com.ra.base_spring_boot.dto.ResponseWrapper;
import com.ra.base_spring_boot.dto.req.BannerRequestDTO;
import com.ra.base_spring_boot.dto.resp.BannerResponseDTO;
import com.ra.base_spring_boot.dto.resp.UserResponseDTO;
import com.ra.base_spring_boot.services.banner.BannerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Banner")
@RestController
@RequestMapping("/api/v1")

public class BannerController {
    @Autowired
    private BannerService bannerService;

    @Operation(summary = "Get all banners", description = "Fetch a paginated list of all banners with sorting and pagination options")
    @GetMapping("/admin/banner")
    public ResponseEntity<?> getBanner(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "limit", defaultValue = "10") int limit,
            @RequestParam(name = "sortBy", defaultValue = "id") String sortBy,
            @RequestParam(name = "orderBy", defaultValue = "asc") String orderBy
    ) {
        Sort sort = orderBy.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, limit, sort);
        Page<BannerResponseDTO> bannerResponseDTOS = bannerService.findAll(pageable);
        if (bannerResponseDTOS.getTotalElements() > 0) {
            return new ResponseEntity<>(bannerResponseDTOS, HttpStatus.OK);
        }
        return new ResponseEntity<>(ResponseWrapper.builder()
                .status(HttpStatus.NO_CONTENT)
                .code(200)
                .data("There are no banner currently.")
                .build(), HttpStatus.NO_CONTENT);
    }

    @Operation(summary = "Get banner by position", description = "Fetch a banner by its position in the layout")
    @GetMapping("/banner/{position}")
    public ResponseEntity<?> getBanner(@PathVariable String position) {
        return ResponseEntity.ok(bannerService.findBannerByPosition(position));
    }
    @Operation(summary = "Get banner by ID", description = "Fetch a banner by its unique ID")
    @GetMapping("/admin/banner/{id}")
    public ResponseEntity<?> getBannerById(@PathVariable Long id) {
        return ResponseEntity.ok(bannerService.findById(id));
    }
    @Operation(summary = "Update a banner", description = "Update an existing banner by ID")
    @PostMapping("/admin/banner/update/{id}")
    public ResponseEntity<?> updateBanner(@PathVariable Long id, @ModelAttribute BannerRequestDTO bannerRequestDTO) {
        return ResponseEntity.ok(bannerService.update(id, bannerRequestDTO));
    }

    @Operation(summary = "Add a new banner", description = "Add a new banner to the system")
    @PostMapping("/admin/banner/add")
    public ResponseEntity<?> addBanner(@ModelAttribute BannerRequestDTO bannerRequestDTO) {
        return ResponseEntity.ok(bannerService.Save(bannerRequestDTO));
    }
    @Operation(summary = "Delete a banner", description = "Delete a banner by its unique ID")
    @DeleteMapping("/admin/banner/delete/{id}")
    public ResponseEntity<?> deleteBanner(@PathVariable Long id) {
        bannerService.delete(id);
        return ResponseEntity.ok().build();
    }


}
