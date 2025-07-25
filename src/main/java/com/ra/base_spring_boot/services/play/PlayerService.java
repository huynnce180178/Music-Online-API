package com.ra.base_spring_boot.services.play;

import com.ra.base_spring_boot.dto.resp.SongHistoryResponseDTO;
import com.ra.base_spring_boot.dto.resp.SongResponseDTO;
import com.ra.base_spring_boot.exception.HttpBadRequest;
import com.ra.base_spring_boot.model.Song;
import com.ra.base_spring_boot.model.SongHistory;
import com.ra.base_spring_boot.model.User;
import com.ra.base_spring_boot.repository.SongRepository;
import com.ra.base_spring_boot.repository.SongHistoryRepository;
import com.ra.base_spring_boot.repository.IUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerService {
    private final SongRepository songRepository;
    private final SongHistoryRepository songHistoryRepository;
    private final IUserRepository userRepository;

    public SongResponseDTO playSongWithViews(Song song) {
        var user = getCurrentUser();
        if (user != null) {
            var history = SongHistory.builder()
                .songs(song)
                .users(user)
                .playedAt(LocalDateTime.now())
                .build();
            songHistoryRepository.save(history);
        }
        return convertToSongDTO(song);
    }

    public void updateSongViews(Song song, double playedDuration) {
        double totalDuration = song.getDuration().getMinutes() * 60.0 + song.getDuration().getSeconds();

        log.info("Song: {} (ID: {})", song.getTitle(), song.getId());
        log.info("Total duration: {} seconds", String.format("%.1f", totalDuration));
        log.info("Played duration: {} seconds", String.format("%.1f", playedDuration));

        // Calculate 80% of total duration
        double minimumPlayTime = totalDuration * 0.8;
        log.info("Minimum required duration (80%): {} seconds", String.format("%.1f", minimumPlayTime));
        log.info("Current views: {}", song.getViews());

        if (playedDuration >= minimumPlayTime) {
            song.setViews(song.getViews() + 1);
            song.setUpdatedAt(LocalDateTime.now());
            songRepository.save(song);
            log.info("View updated. New view count: {}", song.getViews());
        } else {
            log.info("View not counted. Need {} more seconds (minimum: {}, played: {})",
                    String.format("%.1f", minimumPlayTime - playedDuration),
                    String.format("%.1f", minimumPlayTime),
                    String.format("%.1f", playedDuration));
        }
    }

    public <T> SongResponseDTO playInList(List<T> songs, Long currentId, String notFoundMessage,
                                        boolean isNext, T defaultItem) {
        T targetItem = getTargetItem(songs, currentId, notFoundMessage, isNext, defaultItem);
        if (targetItem instanceof Song song) {
            return playSongWithViews(song);
        }
        throw new HttpBadRequest("Unsupported item type");
    }

    public <T> SongHistoryResponseDTO playHistory(List<T> list, Long currentId, boolean isNext) {
        T targetItem = getTargetItem(list, currentId, "Invalid currentId", isNext, null);
        return convertToDTO(targetItem);
    }

    public <T> SongHistoryResponseDTO playCurrent(List<T> list, Long currentId) {
        int currentIndex = findIndexById(list, currentId);
        if (currentIndex == -1) throw new HttpBadRequest("Invalid currentId");
        return convertToDTO(list.get(currentIndex));
    }

    private <T> T getTargetItem(List<T> list, Long currentId, String notFoundMessage,
                               boolean isNext, T defaultItem) {
        int currentIndex = findIndexById(list, currentId);

        if (currentIndex == -1) {
            if (defaultItem != null) return defaultItem;
            throw new HttpBadRequest(notFoundMessage);
        }

        int targetIndex = isNext ?
            (currentIndex + 1) % list.size() :
            (currentIndex - 1 + list.size()) % list.size();

        return list.get(targetIndex);
    }

    private <T> int findIndexById(List<T> list, Long id) {
        for (int i = 0; i < list.size(); i++) {
            Long itemId = null;
            if (list.get(i) instanceof SongHistory sh) itemId = sh.getId();
            else if (list.get(i) instanceof Song s) itemId = s.getId();
            if (itemId != null && itemId.equals(id)) return i;
        }
        return -1;
    }

    private <T> SongHistoryResponseDTO convertToDTO(T item) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        Song song;
        Long historyId = null;
        LocalDateTime playedAt = null;

        if (item instanceof SongHistory sh) {
            song = sh.getSongs();
            historyId = sh.getId();
            playedAt = sh.getPlayedAt();
        } else if (item instanceof Song s) {
            song = s;
        } else {
            throw new HttpBadRequest("Unsupported type");
        }

        String albumName = song.getAlbums() != null ? song.getAlbums().getTitle() : null;
        String playedAtStr = playedAt != null ? playedAt.format(fmt) : "";

        return SongHistoryResponseDTO.builder()
                .historyId(historyId)
                .songId(song.getId())
                .title(song.getTitle())
                .duration(song.getDuration())
                .artistName(song.getUsers().getFirstName() + " " + song.getUsers().getLastName())
                .profilePicUrl(song.getProfilePicUrl())
                .fileUrl(song.getFileUrl())
                .views(song.getViews())
                .playedAtStr(playedAtStr)
                .albumName(albumName)
                .build();
    }

    private SongResponseDTO convertToSongDTO(Song song) {
        Long albumId = song.getAlbums() != null ? song.getAlbums().getId() : null;
        String albumName = song.getAlbums() != null ? song.getAlbums().getTitle() : null;
        Long artistId = song.getUsers() != null ? song.getUsers().getId() : null;
        String artistName = song.getUsers() != null ?
                (song.getUsers().getFirstName() + " " + song.getUsers().getLastName()).trim() : null;

        SongResponseDTO.SongResponseDTOBuilder builder = SongResponseDTO.builder()
                .id(song.getId())
                .title(song.getTitle())
                .duration(song.getDuration())
                .artistName(artistName)
                .profilePicUrl(song.getProfilePicUrl())
                .fileUrl(song.getFileUrl())
                .views(song.getViews())
                .albumName(albumName);

        if (artistId != null) {
            builder.artistId(artistId);
        }
        if (albumId != null) {
            builder.albumId(albumId);
        }

        return builder.build();
    }

    private User getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            Object principal = auth.getPrincipal();
            if (principal instanceof UserDetails ud) {
                return userRepository.findByEmail(ud.getUsername()).orElse(null);
            } else if (principal instanceof User user) {
                return user;
            }
        }
        return null;
    }
}
