package com.ra.base_spring_boot.services.songhistory;

import com.ra.base_spring_boot.dto.resp.SongHistoryResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SongHistoryService {
    Page<SongHistoryResponseDTO> getHistoryByCurrentUser(Pageable pageable);
    void clearSongFromHistory(Long historyId);
    void clearAllHistoryForCurrentUser();
    void addToHistory(Long songId, Long userId);
}
