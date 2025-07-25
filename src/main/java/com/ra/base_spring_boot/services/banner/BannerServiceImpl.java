package com.ra.base_spring_boot.services.banner;

import com.ra.base_spring_boot.dto.req.BannerRequestDTO;
import com.ra.base_spring_boot.dto.resp.BannerResponseDTO;
import com.ra.base_spring_boot.exception.HttpBadRequest;
import com.ra.base_spring_boot.model.Banner;
import com.ra.base_spring_boot.repository.BannerRepository;
import com.ra.base_spring_boot.services.impl.CloudinaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BannerServiceImpl implements BannerService {


    @Autowired
    private BannerRepository bannerRepository;
    @Autowired
    private CloudinaryService cloudinaryService;

    private BannerResponseDTO maptoDTO(Banner banner){
        return BannerResponseDTO.builder()
                .id(banner.getId())
                .profilePicUrl(banner.getUrl())
                .position(banner.getPosition())
                .build();
    }
    private Banner maptoEntity(BannerRequestDTO bannerRequestDTO){
        return Banner.builder()
                .url(cloudinaryService.uploadFile(bannerRequestDTO.getFile(), "images"))
                .position(bannerRequestDTO.getPosition())
                .build();
    }


    @Override
    public BannerResponseDTO Save(BannerRequestDTO bannerRequestDTO) {
        Banner banner = maptoEntity(bannerRequestDTO);
        bannerRepository.save(banner);
        return maptoDTO(banner);
    }

    @Override
    public BannerResponseDTO findById(Long id) {
        Banner banner = bannerRepository.findById(id).orElse(null);
        if (banner == null) {
            throw new HttpBadRequest("Banner not found");
        }return maptoDTO(banner);
    }

        @Override
        public Page<BannerResponseDTO> findAll(Pageable pageable) {

        Page<Banner> banners = bannerRepository.findAll(pageable);
        List<BannerResponseDTO> bannerDTOS = banners.getContent().stream().map(this::maptoDTO).collect(Collectors.toList());
            return new PageImpl<>(bannerDTOS, pageable, banners.getTotalElements());
        }

    @Override
    public BannerResponseDTO findBannerByPosition(String position) {
        Banner banner = bannerRepository.findBannerByPosition(position);
        if (banner == null) {
            throw new HttpBadRequest("Banner not found");
        }
        return maptoDTO(banner);
    }
    @Override
    public BannerResponseDTO update(Long id,BannerRequestDTO bannerRequestDTO) {

        Banner banner = bannerRepository.findById(id).orElse(null);
        if (banner == null) {
            throw new HttpBadRequest("Banner not found");
        }
        banner.setUrl(cloudinaryService.uploadFile(bannerRequestDTO.getFile(), "images"));
        banner.setPosition(bannerRequestDTO.getPosition());
        bannerRepository.save(banner);
        return maptoDTO(banner);
    }
    @Override
    public void delete(Long id) {
        if (bannerRepository.findById(id).orElse(null) == null) {
            throw new HttpBadRequest("Banner not found");
        }
        bannerRepository.deleteById(id);
    }

}
