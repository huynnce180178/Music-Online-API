package com.ra.base_spring_boot.services.play;

import com.ra.base_spring_boot.dto.resp.SongHistoryResponseDTO;
import com.ra.base_spring_boot.dto.resp.SongResponseDTO;
import com.ra.base_spring_boot.exception.HttpBadRequest;
import com.ra.base_spring_boot.exception.HttpNotFound;
import com.ra.base_spring_boot.exception.HttpUnAuthorized;
import com.ra.base_spring_boot.model.*;
import com.ra.base_spring_boot.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlayServiceImpl implements PlayService {
    private final SongRepository songRepository;
    private final IUserRepository userRepository;
    private final SongHistoryRepository songHistoryRepository;
    private final PlaylistSongRepository playlistSongRepository;
    private final PlayerService playerService;
    private final WishlistRepository wishlistRepository;

    @Override
    public SongResponseDTO playSong(Long songId) {
        Song song = songRepository.findById(songId).orElseThrow(() -> new HttpBadRequest("Song not found"));

        if (song.getStatus() != com.ra.base_spring_boot.model.constants.ApprovalStatus.APPROVED) {
            throw new HttpBadRequest("Song is not available for playing");
        }

        return playerService.playSongWithViews(song);
    }

    @Override
    public SongResponseDTO playNextSong(Long currentSongId) {
        List<Song> songs = getAllSongsSorted();
        return playerService.playInList(songs, currentSongId, "Current song not found", true, null);
    }

    @Override
    public SongResponseDTO playPrevSong(Long currentSongId) {
        List<Song> songs = getAllSongsSorted();
        return playerService.playInList(songs, currentSongId, "Current song not found", false, null);
    }

    @Override
    public SongHistoryResponseDTO playSongFromHistory(Long historyId) {
        List<SongHistory> history = getHistoryList();
        return playerService.playCurrent(history, historyId);
    }

    @Override
    public SongHistoryResponseDTO playNextSongOfHistory(Long historyId) {
        List<SongHistory> history = getHistoryList();
        return playerService.playHistory(history, historyId, true);
    }

    @Override
    public SongHistoryResponseDTO playPrevSongOfHistory(Long historyId) {
        List<SongHistory> history = getHistoryList();
        return playerService.playHistory(history, historyId, false);
    }

    @Override
    public SongResponseDTO playSongFromPlaylist(Long songId, Long playlistId) {
        List<Song> songs = getPlaylistSongs(playlistId);
        Song song = songs.stream()
                .filter(s -> s.getId().equals(songId))
                .findFirst()
                .orElseThrow(() -> new HttpBadRequest("Song not found in playlist"));
        return playerService.playSongWithViews(song);
    }

    @Override
    public SongResponseDTO playNextSongFromPlaylist(Long currentSongId, Long playlistId) {
        List<Song> songs = getPlaylistSongs(playlistId);
        return playerService.playInList(songs, currentSongId, "Song not found in playlist", true, songs.get(0));
    }

    @Override
    public SongResponseDTO playPrevSongFromPlaylist(Long currentSongId, Long playlistId) {
        List<Song> songs = getPlaylistSongs(playlistId);
        return playerService.playInList(songs, currentSongId, "Song not found in playlist", false, songs.get(0));
    }


    @Override
    public SongResponseDTO playSongFromWishlist(Long songId) {
        List<Song> songs = getWishlistSongs();
        Song song = songs.stream()
                .filter(s -> s.getId().equals(songId))
                .findFirst()
                .orElseThrow(() -> new HttpBadRequest("Song not found in wishlist"));
        return playerService.playSongWithViews(song);
    }

    @Override
    public SongResponseDTO playNextSongFromWishlist(Long currentSongId) {
        List<Song> songs = getWishlistSongs();
        return playerService.playInList(songs, currentSongId, "Song not found in wishlist", true, songs.get(0));
    }

    @Override
    public SongResponseDTO playPrevSongFromWishlist(Long currentSongId) {
        List<Song> songs = getWishlistSongs();
        return playerService.playInList(songs, currentSongId, "Song not found in wishlist", false, songs.get(0));
    }

    @Override
    public SongResponseDTO playSongByGenre(Long songId, Long genreId) {
        List<Song> songs = getGenreSongs(genreId);
        Song song = songs.stream()
                .filter(s -> s.getId().equals(songId))
                .findFirst()
                .orElseThrow(() -> new HttpBadRequest("Song not found in this genre"));
        return playerService.playSongWithViews(song);
    }

    @Override
    public SongResponseDTO playNextSongByGenre(Long currentSongId, Long genreId) {
        List<Song> songs = getGenreSongs(genreId);
        return playerService.playInList(songs, currentSongId, "Song not found in genre", true, songs.get(0));
    }

    @Override
    public SongResponseDTO playPrevSongByGenre(Long currentSongId, Long genreId) {
        List<Song> songs = getGenreSongs(genreId);
        return playerService.playInList(songs, currentSongId, "Song not found in genre", false, songs.get(0));
    }

    @Override
    public SongResponseDTO playSongFromAlbum(Long songId, Long albumId) {
        List<Song> songs = getAlbumSongs(albumId);
        Song song = songs.stream()
                .filter(s -> s.getId().equals(songId))
                .findFirst()
                .orElseThrow(() -> new HttpNotFound("Song not found in album"));
        return playerService.playSongWithViews(song);
    }

    @Override
    public SongResponseDTO playNextSongFromAlbum(Long currentSongId, Long albumId) {
        List<Song> songs = getAlbumSongs(albumId);
        return playerService.playInList(songs, currentSongId, "Song not found in album", true, songs.get(0));
    }

    @Override
    public SongResponseDTO playPrevSongFromAlbum(Long currentSongId, Long albumId) {
        List<Song> songs = getAlbumSongs(albumId);
        return playerService.playInList(songs, currentSongId, "Song not found in album", false, songs.get(0));
    }

    @Override
    public void updateSongViews(Long songId, double playedDuration) {
        Song song = songRepository.findById(songId)
                .orElseThrow(() -> new HttpBadRequest("Song not found"));
        playerService.updateSongViews(song, playedDuration);
    }

    private List<Song> getAllSongsSorted() {
        return songRepository.findAll().stream()
                .filter(song -> song.getStatus() == com.ra.base_spring_boot.model.constants.ApprovalStatus.APPROVED)
                .sorted(Comparator.comparing(Song::getId))
                .toList();
    }

    private List<SongHistory> getHistoryList() {
        Long userId = getCurrentUserId();
        if (userId == null) throw new HttpUnAuthorized("Unauthorized");
        List<SongHistory> history = songHistoryRepository.findByUsers_IdOrderByPlayedAtAsc(userId);
        if (history.isEmpty()) throw new HttpBadRequest("No song history");
        return history;
    }

    private List<Song> getPlaylistSongs(Long playlistId) {
        List<Song> songs = playlistSongRepository.findSongsByPlaylistId(playlistId)
                .stream()
                .filter(song -> song.getStatus() == com.ra.base_spring_boot.model.constants.ApprovalStatus.APPROVED)
                .sorted(Comparator.comparing(Song::getId))
                .toList();

        if (songs.isEmpty()) {
            throw new HttpBadRequest("Playlist is empty or no approved songs");
        }

        return songs;
    }

    private List<Song> getWishlistSongs() {
        Long userId = getCurrentUserId();
        if (userId == null) throw new HttpUnAuthorized("Unauthorized");
        List<Song> songs = wishlistRepository.findAllByUsers_Id(userId).stream()
                .map(Wishlist::getSongs)
                .filter(song -> song.getStatus() == com.ra.base_spring_boot.model.constants.ApprovalStatus.APPROVED)
                .sorted(Comparator.comparing(Song::getId))
                .toList();
        if (songs.isEmpty()) throw new HttpBadRequest("Wishlist is empty or no approved songs");
        return songs;
    }

    private List<Song> getGenreSongs(Long genreId) {
        List<Song> songs = songRepository.findByGenres_Id(genreId).stream()
                .filter(song -> song.getStatus() == com.ra.base_spring_boot.model.constants.ApprovalStatus.APPROVED)
                .sorted(Comparator.comparing(Song::getId))
                .toList();
        if (songs.isEmpty()) throw new HttpBadRequest("No approved songs found for this genre");
        return songs;
    }

    private List<Song> getAlbumSongs(Long albumId) {
        List<Song> songs = songRepository.findByAlbums_Id(albumId).stream()
                .filter(song -> song.getStatus() == com.ra.base_spring_boot.model.constants.ApprovalStatus.APPROVED)
                .sorted(Comparator.comparing(Song::getId))
                .toList();
        if (songs.isEmpty()) {
            throw new HttpBadRequest("Album is empty or no approved songs");
        }
        return songs;
    }

    private Long getCurrentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            Object principal = auth.getPrincipal();
            if (principal instanceof UserDetails ud) {
                User user = userRepository.findByEmail(ud.getUsername()).orElse(null);
                if (user != null) return user.getId();
            } else if (principal instanceof User user) {
                return user.getId();
            }
        }
        return null;
    }
}
