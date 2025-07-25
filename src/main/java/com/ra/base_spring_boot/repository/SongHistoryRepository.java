package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.model.SongHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SongHistoryRepository extends JpaRepository<SongHistory, Long> {
    Page<SongHistory> findByUsers_Id(Long userId, Pageable pageable);
    SongHistory findByIdAndUsers_Id(Long id, Long userId);
    void deleteByUsers_Id(Long userId);
    List<SongHistory> findByUsers_IdOrderByPlayedAtAsc(Long userId);

    @Query("UPDATE SongHistory sh SET sh.playedAt = CURRENT_TIMESTAMP WHERE sh.users.id = :userId AND sh.songs.id = :songId")
    @org.springframework.data.jpa.repository.Modifying
    void updatePlayedTime(@Param("userId") Long userId, @Param("songId") Long songId);

    List<SongHistory> findAllByUsers_IdAndSongs_Id(Long userId, Long songId);

}
