package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.model.SharedPlaylist;
import com.ra.base_spring_boot.model.User;
import com.ra.base_spring_boot.model.Playlist;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SharedPlaylistRepository extends JpaRepository<SharedPlaylist, Long> {
    List<SharedPlaylist> findByUser_Id(Long userId);
    List<SharedPlaylist> findByPlaylist_Id(Long playlistId);
    boolean existsByPlaylist_IdAndUser_Id(Long playlistId, Long userId);
}
