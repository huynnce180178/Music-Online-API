package com.ra.base_spring_boot.services.songhistory;

import com.ra.base_spring_boot.dto.resp.SongHistoryResponseDTO;
import com.ra.base_spring_boot.exception.HttpBadRequest;
import com.ra.base_spring_boot.exception.HttpNotFound;
import com.ra.base_spring_boot.exception.HttpUnAuthorized;
import com.ra.base_spring_boot.model.Song;
import com.ra.base_spring_boot.model.SongHistory;
import com.ra.base_spring_boot.repository.IUserRepository;
import com.ra.base_spring_boot.repository.SongHistoryRepository;
import com.ra.base_spring_boot.repository.SongRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SongHistoryServiceImpl implements SongHistoryService {
    private final SongHistoryRepository songHistoryRepository;
    private final IUserRepository userRepository;
    private final SongRepository songRepository;

    private static final Map<String, String> SORT_MAPPINGS = new HashMap<>() {{
        put("songId", "songs.id");
        put("title", "songs.title");
        put("artistName", "songs.users.firstName");
        put("playedAt", "playedAt");
    }};

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new HttpUnAuthorized("Authentication required.");
        }
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new HttpNotFound("User not found")).getId();
    }

    @Override
    public Page<SongHistoryResponseDTO> getHistoryByCurrentUser(Pageable pageable) {
        Long userId = getCurrentUserId();
        String sortBy = pageable.getSort().isSorted() ?
                pageable.getSort().iterator().next().getProperty() : "playedAt";
        return getHistoryByUserId(userId, pageable, sortBy);
    }

    public Page<SongHistoryResponseDTO> getHistoryByUserId(Long userId, Pageable pageable, String sortBy) {
        Sort sort = Sort.by(Sort.Direction.fromString(pageable.getSort().getOrderFor(sortBy) != null ?
                Objects.requireNonNull(pageable.getSort().getOrderFor(sortBy)).getDirection().name() : Sort.Direction.DESC.name()),
                SORT_MAPPINGS.getOrDefault(sortBy, sortBy));

        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                sort);

        Page<SongHistory> historyPage = songHistoryRepository.findByUsers_Id(userId, sortedPageable);

        if (historyPage.isEmpty()) {
            throw new HttpNotFound("History of listening to drum music");
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

        return historyPage.map(h -> {
            if (h.getSongs().getStatus() == com.ra.base_spring_boot.model.constants.ApprovalStatus.APPROVED) {
                return SongHistoryResponseDTO.builder()
                        .historyId(h.getId())
                        .songId(h.getSongs().getId())
                        .albumName(h.getSongs().getAlbums() != null ? h.getSongs().getAlbums().getTitle() : null)
                        .title(h.getSongs().getTitle())
                        .duration(h.getSongs().getDuration())
                        .artistName(h.getSongs().getUsers().getFirstName() + " " + h.getSongs().getUsers().getLastName())
                        .fileUrl(h.getSongs().getFileUrl())
                        .profilePicUrl(h.getSongs().getProfilePicUrl())
                        .views(h.getSongs().getViews())
                        .playedAtStr(h.getPlayedAt() != null ? h.getPlayedAt().format(formatter) : null)
                        .build();
            }
            // Trả về DTO với thông tin "not available" cho những bài hát không approved
            return SongHistoryResponseDTO.builder()
                    .historyId(h.getId())
                    .songId(null)
                    .albumName("N/A")
                    .title("Song not available")
                    .duration(null)
                    .artistName("N/A")
                    .fileUrl("")
                    .profilePicUrl("")
                    .views(0L)
                    .playedAtStr(h.getPlayedAt() != null ? h.getPlayedAt().format(formatter) : null)
                    .build();
        });
    }

    @Override
    @Transactional
    public void clearSongFromHistory(Long historyId) {
        Long userId = getCurrentUserId();
        SongHistory history = songHistoryRepository.findByIdAndUsers_Id(historyId, userId);
        if (history == null) {
            throw new HttpNotFound("History entry not found");
        }
        songHistoryRepository.delete(history);
    }

    @Override
    @Transactional
    public void clearAllHistoryForCurrentUser() {
        Long userId = getCurrentUserId();
        songHistoryRepository.deleteByUsers_Id(userId);
    }

    @Override
    @Transactional
    public void addToHistory(Long songId, Long userId) {
        try {
            Song song = songRepository.findById(songId)
                    .orElseThrow(() -> new HttpNotFound("Song not found"));

            if (song.getStatus() != com.ra.base_spring_boot.model.constants.ApprovalStatus.APPROVED) {
                throw new HttpBadRequest("Only approved songs can be added to history");
            }

            List<SongHistory> existingHistories = songHistoryRepository.findAllByUsers_IdAndSongs_Id(userId, songId);
            if (!existingHistories.isEmpty()) {
                songHistoryRepository.deleteAll(existingHistories);
                songHistoryRepository.flush();
            }

            SongHistory newHistory = SongHistory.builder()
                .users(userRepository.findById(userId)
                    .orElseThrow(() -> new HttpNotFound("User not found")))
                .songs(song)
                .playedAt(LocalDateTime.now())
                .build();
            songHistoryRepository.save(newHistory);
            songHistoryRepository.flush();
        } catch (Exception e) {
            throw new RuntimeException("Failed to update song history: " + e.getMessage());
        }
    }
}
