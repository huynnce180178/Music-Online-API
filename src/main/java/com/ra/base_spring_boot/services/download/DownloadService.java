package com.ra.base_spring_boot.services.download;

import com.ra.base_spring_boot.dto.resp.DownloadResponseDTO;
import com.ra.base_spring_boot.dto.req.DownloadRequestDTO;
import com.ra.base_spring_boot.model.Song;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;

public interface DownloadService {
    Page<DownloadResponseDTO> getDownloadsByUserWithPaging(Long userId, Pageable pageable);
    void deleteDownload(Long userId, Long songId);
    DownloadResponseDTO downloadSong(Long userId, DownloadRequestDTO requestDTO);
    byte[] downloadSongFile(Long userId, Long songId);
    HttpHeaders createDownloadHeaders(Song song);
}
