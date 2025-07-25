package com.ra.base_spring_boot.services.playlist;

import com.ra.base_spring_boot.dto.req.PlaylistRequestDTO;
import com.ra.base_spring_boot.dto.resp.AlbumResponseDTO;
import com.ra.base_spring_boot.dto.resp.PlaylistResponseDTO;
import com.ra.base_spring_boot.dto.resp.PublicPlaylistResponseDTO;
import com.ra.base_spring_boot.dto.resp.SongResponseDTO;
import com.ra.base_spring_boot.model.Playlist;


import java.util.List;

public interface PlaylistService {
    PlaylistResponseDTO createPlaylist(PlaylistRequestDTO dto);
    List<PlaylistResponseDTO> searchPlaylists(String keyword);
    PlaylistResponseDTO addSongToPlaylist(Long playlistId, Long songId);
    void removeSongFromPlaylist(Long playlistId, Long songId);
    List<PlaylistResponseDTO> getAllPlaylists();
    List<PublicPlaylistResponseDTO> getPublicPlaylists();
    PlaylistResponseDTO setPlaylistPublic(Long playlistId, boolean isPublic);
    PlaylistResponseDTO updatePlaylist(Long playlistId, PlaylistRequestDTO dto);
    void deletePlaylist(Long playlistId);
    List<SongResponseDTO> getSongsByPlaylistId(Long playlistId);
    List<SongResponseDTO> getSongsByPlaylistIdForGuest(Long playlistId);
    String generateShareCode(Long playlistId);
    void receiveSharedPlaylist(String shareCode);
    List<PlaylistResponseDTO> getReceivedSharedPlaylists();
    void deleteReceivedSharedPlaylist(Long playlistId);
    PlaylistResponseDTO getPlaylistByCurrentUserAgeGroup();
    List<PlaylistResponseDTO> fullTextSearchPlaylist(String keyword);
}
