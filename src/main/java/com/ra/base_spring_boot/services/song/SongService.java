package com.ra.base_spring_boot.services.song;

import com.ra.base_spring_boot.dto.req.SongRequestDTO;
import com.ra.base_spring_boot.dto.resp.SongDetailDTO;
import com.ra.base_spring_boot.dto.resp.SongResponseDTO;
import com.ra.base_spring_boot.model.User;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;

public interface SongService {
    // Artist operations
    SongResponseDTO addSong(SongRequestDTO songRequestDTO, Authentication authentication);
    SongResponseDTO updateSong(Long songId, SongRequestDTO songRequestDTO, Authentication authentication);
    void deleteByArtist(Long songId);
    List<SongResponseDTO> getMySongs(Authentication authentication);
    List<SongResponseDTO> getMyRejectedSongs(Authentication authentication);
    List<SongResponseDTO> getMySelfDeletedSongs(Authentication authentication);
    List<SongResponseDTO> getMyDeletedByAdminSongs(Authentication authentication);
    void requestRestoreSong(Long songId);
    List<SongResponseDTO> getApprovedSongsByAlbum(Long albumId);
    void removeSongFromAlbum(Long songId);
    void addSongToAlbum(Long songId, Long albumId);
    String getLyricById(Long id);

    void deleteSongByAdmin(Long id, String reason);
    void approveSong(Long songId);
    void rejectSong(Long songId, String reason);
    void restoreSong(Long songId);
    List<SongResponseDTO> getPendingSongs();
    List<SongResponseDTO> getRejectedSongs();
    List<SongResponseDTO> getDeletedByArtistSongs();
    List<SongResponseDTO> getDeletedByAdminSongs();

    List<SongResponseDTO> getAllSongs();
    Long countSong(LocalDateTime start, LocalDateTime end);
    User getUserByUsername(String username);
    SongDetailDTO getSongDetailForGuest(Long songId);
    SongDetailDTO getSongDetailWithAuth(Long songId, Authentication authentication);
    List<SongResponseDTO> getSongsByArtistId(Long artistId);
    List<SongResponseDTO> fullTextSearchSong(String keyword);
    List<SongResponseDTO> getMyPendingSongs(Authentication authentication);
    Long getUserIdFromAuthentication(Authentication authentication);
}
