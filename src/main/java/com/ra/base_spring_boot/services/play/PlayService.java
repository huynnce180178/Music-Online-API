package com.ra.base_spring_boot.services.play;

import com.ra.base_spring_boot.dto.resp.SongResponseDTO;
import com.ra.base_spring_boot.dto.resp.SongHistoryResponseDTO;

public interface PlayService {
    // Play theo danh sách Song
    SongResponseDTO playSong(Long songId);
    SongResponseDTO playNextSong(Long currentSongId);
    SongResponseDTO playPrevSong(Long currentSongId);

    // Cập nhật lượt nghe
    void updateSongViews(Long songId, double playedDuration);

    // Play theo danh sách History
    SongHistoryResponseDTO playSongFromHistory(Long historyId);
    SongHistoryResponseDTO playNextSongOfHistory(Long historyId);
    SongHistoryResponseDTO playPrevSongOfHistory(Long historyId);

    // Play theo danh sách Playlist
    SongResponseDTO playSongFromPlaylist(Long songId, Long playlistId);
    SongResponseDTO playNextSongFromPlaylist(Long currentSongId, Long playlistId);
    SongResponseDTO playPrevSongFromPlaylist(Long currentSongId, Long playlistId);

    // Play theo danh sách Wishlist
    SongResponseDTO playSongFromWishlist(Long songId);
    SongResponseDTO playNextSongFromWishlist(Long currentSongId);
    SongResponseDTO playPrevSongFromWishlist(Long currentSongId);

    // Play theo danh sách Genre
    SongResponseDTO playSongByGenre(Long songId, Long genreId);
    SongResponseDTO playNextSongByGenre(Long currentSongId, Long genreId);
    SongResponseDTO playPrevSongByGenre(Long currentSongId, Long genreId);

    // Play theo danh sách Album
    SongResponseDTO playSongFromAlbum(Long songId, Long albumId);
    SongResponseDTO playNextSongFromAlbum(Long currentSongId, Long albumId);
    SongResponseDTO playPrevSongFromAlbum(Long currentSongId, Long albumId);
}
