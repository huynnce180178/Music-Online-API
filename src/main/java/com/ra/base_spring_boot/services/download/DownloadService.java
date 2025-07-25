package com.ra.base_spring_boot.services.download;

import com.ra.base_spring_boot.dto.resp.DownloadResponseDTO;
import com.ra.base_spring_boot.dto.req.DownloadRequestDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface DownloadService {
    Page<DownloadResponseDTO> getDownloadsByUserWithPaging(Long userId, Pageable pageable);
    void deleteDownload(Long userId, Long songId);
    DownloadResponseDTO downloadSong(Long userId, DownloadRequestDTO requestDTO);
    void downloadSongFile(Long userId, Long songId);
}
