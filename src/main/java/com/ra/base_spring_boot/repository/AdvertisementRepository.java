package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.model.Advertisement;
import com.ra.base_spring_boot.model.constants.ApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AdvertisementRepository extends JpaRepository<Advertisement, Long> {
    List<Advertisement> findByStatus(ApprovalStatus status);
    List<Advertisement> findByEndDateBefore(LocalDate date);
}
