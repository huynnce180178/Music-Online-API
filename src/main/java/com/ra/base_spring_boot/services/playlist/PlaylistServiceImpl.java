package com.ra.base_spring_boot.services.playlist;

import com.ra.base_spring_boot.dto.req.PlaylistRequestDTO;
import com.ra.base_spring_boot.dto.resp.AlbumResponseDTO;
import com.ra.base_spring_boot.dto.resp.PlaylistResponseDTO;
import com.ra.base_spring_boot.dto.resp.PublicPlaylistResponseDTO;
import com.ra.base_spring_boot.dto.resp.SongResponseDTO;
import com.ra.base_spring_boot.exception.HttpBadRequest;
import com.ra.base_spring_boot.exception.HttpNotFound;
import com.ra.base_spring_boot.exception.HttpUnAuthorized;
import com.ra.base_spring_boot.model.Playlist;
import com.ra.base_spring_boot.model.PlaylistSong;
import com.ra.base_spring_boot.model.Song;
import com.ra.base_spring_boot.model.User;
import com.ra.base_spring_boot.model.SharedPlaylist;
import com.ra.base_spring_boot.model.constants.AgeGroup;
import javax.annotation.PostConstruct;
import com.ra.base_spring_boot.model.*;
import com.ra.base_spring_boot.repository.IUserRepository;
import com.ra.base_spring_boot.repository.PlaylistRepository;
import com.ra.base_spring_boot.repository.PlaylistSongRepository;
import com.ra.base_spring_boot.repository.SongRepository;
import com.ra.base_spring_boot.repository.SharedPlaylistRepository;
import com.ra.base_spring_boot.services.impl.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlaylistServiceImpl implements PlaylistService {
    private final PlaylistRepository playlistRepository;
    private final PlaylistSongRepository playlistSongRepository;
    private final SongRepository songRepository;
    private final IUserRepository userRepository;
    private final SharedPlaylistRepository sharedPlaylistRepository;
    @Autowired
    private CloudinaryService cloudinaryService;

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new HttpUnAuthorized("User not authenticated");
        }
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new HttpNotFound("User not found")).getId();
    }

    @Override
    @Transactional
    public PlaylistResponseDTO createPlaylist(PlaylistRequestDTO dto) {
        Long userId = getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new HttpNotFound("User not found with ID: " + userId));

        String uploadedImageUrl = dto.getImageUrl() != null && !dto.getImageUrl().isEmpty()
                ? cloudinaryService.uploadFile(dto.getImageUrl(), "images")
                : null;

        Playlist playlist = Playlist.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .isPublic(dto.getIsPublic() != null ? dto.getIsPublic() : false)
                .imageUrl(uploadedImageUrl)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .user(user)
                .build();

        Playlist saved = playlistRepository.save(playlist);
        return convertToPlaylistDTO(saved, new ArrayList<>());
    }

    @Override
    @Transactional
    public PlaylistResponseDTO updatePlaylist(Long playlistId, PlaylistRequestDTO dto) {
        Long userId = getCurrentUserId();
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new HttpNotFound("Playlist not found with ID: " + playlistId));

        if (!playlist.getUser().getId().equals(userId)) {
            throw new HttpBadRequest("You do not have permission to update this playlist.");
        }

        if (dto.getName() != null) playlist.setName(dto.getName());
        if (dto.getDescription() != null) playlist.setDescription(dto.getDescription());
        if (dto.getIsPublic() != null) playlist.setPublic(dto.getIsPublic());

        if (dto.getImageUrl() != null && !dto.getImageUrl().isEmpty()) {
            String newImageUrl = cloudinaryService.uploadFile(dto.getImageUrl(), "images");
            playlist.setImageUrl(newImageUrl);
        }

        playlist.setUpdatedAt(LocalDateTime.now());
        Playlist updated = playlistRepository.save(playlist);
        return convertToPlaylistDTO(updated, getSongsOfPlaylist(updated));
    }


    @Override
    public List<PlaylistResponseDTO> searchPlaylists(String keyword) {
        List<Playlist> playlists = playlistRepository.findAll();
        List<PlaylistResponseDTO> results = playlists.stream()
                .filter(p -> p.getName().toLowerCase().contains(keyword.toLowerCase()))
                .map(p -> convertToPlaylistDTO(p, getSongsOfPlaylist(p)))
                .collect(Collectors.toList());
        if (results.isEmpty()) {
            throw new HttpNotFound("No playlists found with keyword: " + keyword);
        }
        return results;
    }

    @Override
    @Transactional
    public PlaylistResponseDTO addSongToPlaylist(Long playlistId, Long songId) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new HttpNotFound("Playlist not found with ID: " + playlistId));
        Song song = songRepository.findById(songId)
                .orElseThrow(() -> new HttpNotFound("Song not found with ID: " + songId));

        if (song.getStatus() != com.ra.base_spring_boot.model.constants.ApprovalStatus.APPROVED) {
            throw new HttpBadRequest("Only approved songs can be added to playlist.");
        }

        boolean exists = playlistSongRepository.findByPlaylists_IdAndSongs_Id(playlistId, songId).isPresent();
        if (exists) {
            throw new HttpBadRequest("Song with ID: " + songId + " is already in playlist with ID: " + playlistId);
        }
        PlaylistSong ps = PlaylistSong.builder()
                .playlists(playlist)
                .songs(song)
                .addedAt(LocalDateTime.now())
                .build();
        playlistSongRepository.save(ps);
        return convertToPlaylistDTO(playlist, getSongsOfPlaylist(playlist));
    }

    @Override
    @Transactional
    public void removeSongFromPlaylist(Long playlistId, Long songId) {
        Long userId = getCurrentUserId();
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new HttpNotFound("Playlist not found with ID: " + playlistId));
        if (!playlist.getUser().getId().equals(userId)) {
            throw new HttpUnAuthorized("Only the playlist owner can remove songs from this playlist");
        }
        PlaylistSong playlistSong = playlistSongRepository.findByPlaylists_IdAndSongs_Id(playlistId, songId)
                .orElseThrow(() -> new HttpNotFound("Song with ID: " + songId + " not found in playlist with ID: " + playlistId));
        playlistSongRepository.delete(playlistSong);
    }

    @Override
    public List<PlaylistResponseDTO> getAllPlaylists() {
        Long userId = getCurrentUserId();
        List<Playlist> ownedPlaylists = playlistRepository.findAllByUser_Id(userId);
        List<Playlist> sharedPlaylists = sharedPlaylistRepository.findByUser_Id(userId)
                .stream()
                .map(SharedPlaylist::getPlaylist)
                .collect(Collectors.toList());
        List<Playlist> allPlaylists = new ArrayList<>(ownedPlaylists);
        for (Playlist p : sharedPlaylists) {
            if (allPlaylists.stream().noneMatch(op -> op.getId().equals(p.getId()))) {
                allPlaylists.add(p);
            }
        }
        if (allPlaylists.isEmpty()) {
            throw new HttpNotFound("No playlists found for user.");
        }
        return allPlaylists.stream()
                .map(p -> convertToPlaylistDTO(p, getSongsOfPlaylist(p)))
                .collect(Collectors.toList());
    }

    @Override
    public List<PublicPlaylistResponseDTO> getPublicPlaylists() {
        List<Playlist> playlists = playlistRepository.findAllByIsPublicTrue();
        if (playlists.isEmpty()) {
            throw new HttpNotFound("No public playlists found.");
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        return playlists.stream()
                .map(p -> PublicPlaylistResponseDTO.builder()
                        .id(p.getId())
                        .name(p.getName())
                        .description(p.getDescription())
                        .isPublic(p.isPublic())
                        .imageUrl(p.getImageUrl())
                        .createdAtStr(p.getCreatedAt() != null ? p.getCreatedAt().format(formatter) : null)
                        .updatedAtStr(p.getUpdatedAt() != null ? p.getUpdatedAt().format(formatter) : null)
                        .userId(p.getUser().getId())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public PlaylistResponseDTO setPlaylistPublic(Long playlistId, boolean isPublic) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new HttpNotFound("Playlist not found with ID: " + playlistId));
        playlist.setPublic(isPublic);
        playlist.setUpdatedAt(java.time.LocalDateTime.now());
        Playlist updated = playlistRepository.save(playlist);
        return convertToPlaylistDTO(updated, getSongsOfPlaylist(updated));
    }


    @Override
    @Transactional
    public void deletePlaylist(Long playlistId) {
        Long userId = getCurrentUserId();
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new HttpNotFound("Playlist not found with ID: " + playlistId));
        if (!playlist.getUser().getId().equals(userId)) {
            throw new HttpBadRequest("You do not have permission to delete this playlist.");
        }
        List<PlaylistSong> playlistSongs = playlistSongRepository.findByPlaylists_Id(playlistId);
        playlistSongRepository.deleteAll(playlistSongs);
        playlistRepository.deleteById(playlistId);
    }

    @Transactional
    public void sharePlaylistToUser(Long playlistId, Long targetUserId) {
        Long currentUserId = getCurrentUserId();
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new HttpNotFound("Playlist not found with ID: " + playlistId));
        if (!playlist.getUser().getId().equals(currentUserId)) {
            throw new HttpBadRequest("You do not have permission to share this playlist.");
        }
        if (playlist.isPublic()) {
            throw new HttpBadRequest("Public playlists do not need to be shared.");
        }
        if (sharedPlaylistRepository.existsByPlaylist_IdAndUser_Id(playlistId, targetUserId)) {
            throw new HttpBadRequest("Playlist already shared with this user.");
        }
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new HttpNotFound("User not found with ID: " + targetUserId));
        SharedPlaylist shared = SharedPlaylist.builder()
                .playlist(playlist)
                .user(targetUser)
                .build();
        sharedPlaylistRepository.save(shared);
    }

    public String generateShareCode(Long playlistId) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new HttpNotFound("Playlist not found with ID: " + playlistId));
        String uuid = java.util.UUID.randomUUID().toString().replace("-", "");
        String code = uuid + ":" + playlistId;
        return java.util.Base64.getEncoder().encodeToString(code.getBytes());
    }

    @Transactional
    public void receiveSharedPlaylist(String shareCode) {
        String decoded;
        try {
            decoded = new String(java.util.Base64.getDecoder().decode(shareCode));
        } catch (Exception e) {
            throw new HttpBadRequest("Invalid share code");
        }
        String[] parts = decoded.split(":");
        if (parts.length != 2) {
            throw new HttpBadRequest("Invalid share code");
        }
        Long playlistId;
        try {
            playlistId = Long.parseLong(parts[1]);
        } catch (Exception e) {
            throw new HttpBadRequest("Invalid share code");
        }
        Playlist originalPlaylist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new HttpNotFound("Playlist not found with ID: " + playlistId));
        Long userId = getCurrentUserId();
        if (originalPlaylist.getUser().getId().equals(userId)) {
            throw new HttpBadRequest("You already own this playlist");
        }
        String ownerName = originalPlaylist.getUser().getFirstName() != null ? originalPlaylist.getUser().getFirstName() : "";
        if (originalPlaylist.getUser().getLastName() != null) {
            ownerName = ownerName.isEmpty() ? originalPlaylist.getUser().getLastName() : ownerName + " " + originalPlaylist.getUser().getLastName();
        }
        String newPlaylistName = "Play of " + ownerName;
        User user = userRepository.findById(userId).orElseThrow(() -> new HttpNotFound("User not found"));
        Playlist newPlaylist = Playlist.builder()
                .name(newPlaylistName)
                .description(originalPlaylist.getDescription())
                .isPublic(false)
                .imageUrl(originalPlaylist.getImageUrl())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .user(user)
                .build();
        Playlist savedPlaylist = playlistRepository.save(newPlaylist);
        // Copy PlaylistSong
        List<PlaylistSong> originalSongs = playlistSongRepository.findByPlaylists_Id(originalPlaylist.getId());
        for (PlaylistSong ps : originalSongs) {
            PlaylistSong newPs = PlaylistSong.builder()
                    .playlists(savedPlaylist)
                    .songs(ps.getSongs())
                    .addedAt(LocalDateTime.now())
                    .build();
            playlistSongRepository.save(newPs);
        }
    }

    public List<PlaylistResponseDTO> getReceivedSharedPlaylists() {
        Long userId = getCurrentUserId();
        List<Playlist> sharedPlaylists = sharedPlaylistRepository.findByUser_Id(userId)
                .stream()
                .map(SharedPlaylist::getPlaylist)
                .filter(p -> !p.getUser().getId().equals(userId))
                .collect(Collectors.toList());
        return sharedPlaylists.stream()
                .map(p -> convertToPlaylistDTO(p, getSongsOfPlaylist(p)))
                .collect(Collectors.toList());
    }

    private String getPlaylistNameForAgeGroup(AgeGroup ageGroup) {
        return switch (ageGroup) {
            case TEENAGER -> "Teenify";
            case YOUNG_ADULT -> "Youngify";
            case ADULT -> "Adultify";
            case MIDDLE_AGE -> "Midify";
            case MATURE_ADULT -> "Maturify";
            case OLDER_ADULT -> "Oldify";
            case SENIOR -> "Goldify";
        };
    }

    @PostConstruct
    public void initDefaultPlaylistsByAgeGroup() {
        User defaultUser = userRepository.findAll().stream().findFirst().orElse(null);
        if (defaultUser == null) {
            System.err.println("No user found to assign to default playlist!");
            return;
        }
        for (AgeGroup ageGroup : AgeGroup.values()) {
            String playlistName = getPlaylistNameForAgeGroup(ageGroup);
            if (!playlistRepository.existsByName(playlistName)) {
                Playlist playlist = Playlist.builder()
                        .name(playlistName)
                        .description("Playlist " + ageGroup.name())
                        .isPublic(true)
                        .user(defaultUser)
                        .createdAt(java.time.LocalDateTime.now())
                        .updatedAt(java.time.LocalDateTime.now())
                        .build();
                playlistRepository.save(playlist);
            }
        }
    }

    public Playlist getPlaylistForUserAgeGroup(User user) {
        int age = user.getAge();
        AgeGroup userAgeGroup = null;
        if (age >= 13 && age <= 17) userAgeGroup = AgeGroup.TEENAGER;
        else if (age >= 18 && age <= 24) userAgeGroup = AgeGroup.YOUNG_ADULT;
        else if (age >= 25 && age <= 34) userAgeGroup = AgeGroup.ADULT;
        else if (age >= 35 && age <= 44) userAgeGroup = AgeGroup.MIDDLE_AGE;
        else if (age >= 45 && age <= 54) userAgeGroup = AgeGroup.MATURE_ADULT;
        else if (age >= 55 && age <= 64) userAgeGroup = AgeGroup.OLDER_ADULT;
        else if (age >= 65) userAgeGroup = AgeGroup.SENIOR;
        if (userAgeGroup == null) throw new HttpNotFound("Unable to find age group for user");
        final AgeGroup finalUserAgeGroup = userAgeGroup;
        String playlistName = getPlaylistNameForAgeGroup(finalUserAgeGroup);
        return playlistRepository.findByName(playlistName)
                .orElseThrow(() -> new HttpNotFound("Unable to find playlist for age group: " + finalUserAgeGroup.name()));
    }


    public Playlist getPlaylistForCurrentUserAgeGroup() {
        Long userId = getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new HttpNotFound("User not found with ID: " + userId));
        return getPlaylistForUserAgeGroup(user);
    }

    @Override
    public PlaylistResponseDTO getPlaylistByCurrentUserAgeGroup() {
        Playlist playlist = getPlaylistForCurrentUserAgeGroup();
        List<SongResponseDTO> songs = getSongsOfPlaylist(playlist);
        return convertToPlaylistDTO(playlist, songs);
    }

    private List<SongResponseDTO> getSongsOfPlaylist(Playlist playlist) {
        // Corrected call to findByPlaylists_Id
        List<PlaylistSong> playlistSongs = playlistSongRepository.findByPlaylists_Id(playlist.getId());
        return playlistSongs.stream()
                .map(PlaylistSong::getSongs)
                .filter(song -> song.getStatus() == com.ra.base_spring_boot.model.constants.ApprovalStatus.APPROVED)
                .map(this::convertToSongDTO)
                .collect(Collectors.toList());
    }

    private PlaylistResponseDTO convertToPlaylistDTO(Playlist playlist, List<SongResponseDTO> songs) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        return PlaylistResponseDTO.builder()
                .id(playlist.getId())
                .name(playlist.getName())
                .description(playlist.getDescription())
                .isPublic(playlist.isPublic())
                .imageUrl(playlist.getImageUrl())
                .createdAtStr(playlist.getCreatedAt() != null ? playlist.getCreatedAt().format(formatter) : null)
                .updatedAtStr(playlist.getUpdatedAt() != null ? playlist.getUpdatedAt().format(formatter) : null)
                .userId(playlist.getUser().getId())
                .songs(songs)
                .build();
    }


    private SongResponseDTO convertToSongDTO(Song song) {
        String artistName = null;
        Long artistId = null;
        if (song.getUsers() != null) {
            artistId = song.getUsers().getId();
            String firstName = song.getUsers().getFirstName();
            String lastName = song.getUsers().getLastName();
            String tempArtistName = (firstName != null ? firstName : "") + (lastName != null ? (firstName != null ? " " : "") + lastName : "");
            artistName = tempArtistName.trim().isEmpty() ? null : tempArtistName.trim();
        }

        return SongResponseDTO.builder()
                .id(song.getId())
                .title(song.getTitle())
                .duration(song.getDuration())
                .artistId(artistId)
                .artistName(artistName)
                .albumId(song.getAlbums() != null ? song.getAlbums().getId() : null)
                .albumName(song.getAlbums() != null ? song.getAlbums().getTitle() : null)
                .profilePicUrl(song.getProfilePicUrl())
                .fileUrl(song.getFileUrl())
                .views(song.getViews())
                .build();
    }

    @Override
    public List<SongResponseDTO> getSongsByPlaylistId(Long playlistId) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new HttpNotFound("Playlist not found with ID: " + playlistId));
        Long userId = getCurrentUserId();
        boolean isOwner = playlist.getUser().getId().equals(userId);
        boolean isShared = sharedPlaylistRepository.existsByPlaylist_IdAndUser_Id(playlistId, userId);
        if (!playlist.isPublic() && !isOwner && !isShared) {
            throw new HttpUnAuthorized("You do not have permission to view this playlist");
        }
        return getSongsOfPlaylist(playlist);
    }

    public List<SongResponseDTO> getSongsByPlaylistIdForGuest(Long playlistId) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new HttpNotFound("Playlist not found with ID: " + playlistId));
        if (!playlist.isPublic()) {
            throw new HttpUnAuthorized("You do not have permission to view this playlist as guest");
        }
        return getSongsOfPlaylist(playlist);
    }

    @Transactional
    public void deleteReceivedSharedPlaylist(Long playlistId) {
        Long userId = getCurrentUserId();
        SharedPlaylist shared = sharedPlaylistRepository.findByPlaylist_Id(playlistId).stream()
                .filter(s -> s.getUser().getId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new HttpNotFound("Shared playlist not found for this user"));
        sharedPlaylistRepository.delete(shared);
    }
    @Override
    public List<PlaylistResponseDTO> fullTextSearchPlaylist(String keyword) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

        return playlistRepository.fullTextSearchPlaylists(keyword)
                .stream()
                .map(p -> PlaylistResponseDTO.builder()
                        .id(p.getId())
                        .name(p.getName())
                        .description(p.getDescription())
                        .isPublic(p.isPublic())
                        .imageUrl(p.getImageUrl())
                        .createdAtStr(p.getCreatedAt() != null ? p.getCreatedAt().format(formatter) : null)
                        .updatedAtStr(p.getUpdatedAt() != null ? p.getUpdatedAt().format(formatter) : null)
                        .userId(p.getUser().getId())
                        .build())
                .toList();
    }
}
