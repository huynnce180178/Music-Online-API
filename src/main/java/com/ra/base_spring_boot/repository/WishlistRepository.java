package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.model.Wishlist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    Page<Wishlist> findByUsers_Id(Long userId, Pageable pageable);
    List<Wishlist> findAllByUsers_Id(Long userId);
    Wishlist findByUsers_IdAndSongs_Id(Long userId, Long songId);
    void deleteByUsers_Id(Long userId);
    Long countWishlistBySongs_Id(Long songId);
}
