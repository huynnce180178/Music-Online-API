package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.model.Playlist;
import com.ra.base_spring_boot.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PlaylistRepository extends JpaRepository<Playlist, Long> {
    List<Playlist> findAllByUser_Id(Long userId);
    List<Playlist> findAllByIsPublicTrue();

    Playlist findByNameAndUser(String name, User user);
    Optional<Playlist> findByName(String name);
    boolean existsByName(String name);

    @Query(value = """
    SELECT *,
      MATCH(description) AGAINST (?1 IN BOOLEAN MODE) AS relevance
    FROM playlists p
    WHERE MATCH(description) AGAINST (?1 IN BOOLEAN MODE)
      AND is_public = true
    ORDER BY relevance DESC
""", nativeQuery = true)
    List<Playlist> fullTextSearchPlaylists(String keyword);



}
