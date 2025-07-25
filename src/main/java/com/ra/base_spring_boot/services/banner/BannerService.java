package com.ra.base_spring_boot.services.banner;

import com.ra.base_spring_boot.dto.req.BannerRequestDTO;
import com.ra.base_spring_boot.dto.resp.BannerResponseDTO;
import com.ra.base_spring_boot.model.Banner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface BannerService {
    BannerResponseDTO Save(BannerRequestDTO bannerRequestDTO);
    BannerResponseDTO findById(Long id);
    Page<BannerResponseDTO> findAll(Pageable pageable);
    BannerResponseDTO findBannerByPosition(String position);
    BannerResponseDTO update(Long id,BannerRequestDTO bannerRequestDTO);
    void delete(Long id);

}
