package com.ra.base_spring_boot.repository;


import com.ra.base_spring_boot.model.SongProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SongProfileRepository extends JpaRepository<SongProfile, Long> {
    SongProfile findBySongId(Long songId);
    boolean existsBySongId(Long songId);

}
