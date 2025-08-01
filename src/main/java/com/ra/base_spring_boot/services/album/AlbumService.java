package com.ra.base_spring_boot.services.album;

import com.ra.base_spring_boot.dto.req.AlbumRequestDTO;
import com.ra.base_spring_boot.dto.resp.AlbumResponseDTO;
import com.ra.base_spring_boot.dto.resp.AlbumDetailResponseDTO;
import com.ra.base_spring_boot.model.Album;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.core.Authentication;

public interface AlbumService {
    // Artist operations
    List<AlbumResponseDTO> getAllAlbums();
    List<AlbumResponseDTO> getAlbumsByArtistId(Long artistId);
    AlbumResponseDTO createAlbum(AlbumRequestDTO albumRequestDTO, Authentication authentication);
    AlbumResponseDTO updateAlbum(Long id, AlbumRequestDTO albumRequestDTO, Authentication authentication);
    void deleteAlbum(Long id);
    List<AlbumResponseDTO> getMyDeletedAlbums(Authentication authentication);
    List<AlbumResponseDTO> getMyRejectedAlbums(Authentication authentication);
    List<AlbumResponseDTO> getMyDeletedByAdminAlbums(Authentication authentication);
    void requestRestoreAlbum(Long albumId);
    List<AlbumResponseDTO> getMyPendingAlbums(Authentication authentication);

    // Admin operations
    List<AlbumResponseDTO> getPendingAlbums();
    List<AlbumResponseDTO> getRejectedAlbums();
    List<AlbumResponseDTO> getDeletedByAdminAlbums();
    void approveAlbum(Long albumId);
    void rejectAlbum(Long albumId, String reason);
    void restoreAlbum(Long albumId);
    void deleteAlbumByAdmin(Long albumId, String reason);

    // Common operations
    List<AlbumResponseDTO> searchAlbumsByTitle(String title);
    List<AlbumResponseDTO> fullTextSearchAlbums(String keyword);
    Long countAlbum(LocalDateTime start, LocalDateTime end);
    Album getAlbumWithArtist(Long albumId);
    AlbumDetailResponseDTO getAlbumDetailWithOwner(Long albumId);
}
