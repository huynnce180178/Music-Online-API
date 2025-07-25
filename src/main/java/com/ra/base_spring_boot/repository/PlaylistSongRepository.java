package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.model.PlaylistSong;
import com.ra.base_spring_boot.model.Song;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional; // Import Optional

@Repository
public interface PlaylistSongRepository extends JpaRepository<PlaylistSong, Long> {

    Optional<PlaylistSong> findByPlaylists_IdAndSongs_Id(Long playlistId, Long songId);

    List<PlaylistSong> findByPlaylists_Id(Long playlistId);

    @Query("SELECT ps.songs FROM PlaylistSong ps WHERE ps.playlists.id = :playlistId")
    List<Song> findSongsByPlaylistId(@Param("playlistId") Long playlistId);

    @Modifying
    @Transactional
    @Query("""
        delete from PlaylistSong ps where ps.playlists.id = :playlistId
""")
    void deleteAllByPlaylistId(@Param("playlistId") Long playlistId);
}