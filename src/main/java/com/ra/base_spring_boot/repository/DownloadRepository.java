package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.model.Download;
import com.ra.base_spring_boot.model.Song;
import com.ra.base_spring_boot.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DownloadRepository extends JpaRepository<Download, Long> {
    List<Download> findByUsers(User user);

    @Query("SELECT d FROM Download d JOIN FETCH d.songs s LEFT JOIN FETCH s.albums WHERE d.users = :user")
    List<Download> findByUsersWithSongs(@Param("user") User user);

    @Query(value = "SELECT d FROM Download d JOIN FETCH d.songs s LEFT JOIN FETCH s.albums WHERE d.users = :user",
           countQuery = "SELECT COUNT(d) FROM Download d WHERE d.users = :user")
    Page<Download> findByUsersWithSongs(@Param("user") User user, Pageable pageable);

    Optional<Download> findByUsersAndSongs(User user, Song song);

    List<Download> findAllByUsersAndSongs(User users, Song songs);
}
