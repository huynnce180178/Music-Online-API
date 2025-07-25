package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.model.Banner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BannerRepository extends JpaRepository<Banner, Long> {

    Banner findBannerByPosition(String position);
    Banner findFirstByPosition(String position);
}
