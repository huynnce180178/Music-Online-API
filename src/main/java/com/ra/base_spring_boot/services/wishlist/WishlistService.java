package com.ra.base_spring_boot.services.wishlist;

import com.ra.base_spring_boot.dto.req.WishlistRequestDTO;
import com.ra.base_spring_boot.dto.resp.WishlistResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface WishlistService {
    WishlistResponseDTO addToWishlist(WishlistRequestDTO requestDTO);
    String removeFromWishlist(Long songId);
    Page<WishlistResponseDTO> getWishlistByCurrentUser(Pageable pageable);
    Page<WishlistResponseDTO> getWishlistByUserId(Long userId, Pageable pageable, String sortBy);
    Long countLikeBySongId(Long songId);
}
