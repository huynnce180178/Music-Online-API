package com.ra.base_spring_boot.services.guest;

import com.ra.base_spring_boot.dto.resp.*;
import jakarta.servlet.http.HttpSession;

import java.util.List;

public interface GuestService {
    List<TopArtistResponseDTO> findTop10ArtistsByTotalViews();
    List<TopGenreResponseDTO> findTop10GenresByTotalViews();
    List<TopAlbumResponseDTO> findTop10AlbumsByTotalViews();
    List<TopSongOfWeekResponseDTO> findTopSongsOfWeekByTotalViews();

    void savePlayedSongForGuest(int idSong, HttpSession session);
    List<SongResponseDTO> getRecentlyPlayedSongs(HttpSession session);

    List<GuestAlbumResponseDTO> getAllAlbums();
    List<FeaturedAlbumResponseDTO> getFeaturedAlbums();
    List<Top15AlbumsResponseDTO> getTop15Albums();
    List<GuestAlbumResponseDTO> getAlbumsByArtist(Long artistId);

    List<GuestAlbumResponseDTO> getOneTopAlbumPerArtist();

    List<GuestAlbumResponseDTO> getNewlyReleasedAlbums();

    List<SongResponseDTO> findTopSongsByTotalViews();
    List<SongResponseDTO> getTopSongsInMonth();
    FullTextSearchResponseDTO getFullTextSearchResponseDTO(String keyword);
}
