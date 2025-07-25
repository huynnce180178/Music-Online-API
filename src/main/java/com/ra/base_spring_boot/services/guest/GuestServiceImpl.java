package com.ra.base_spring_boot.services.guest;

import com.ra.base_spring_boot.dto.resp.*;
import com.ra.base_spring_boot.exception.HttpNotFound;
import com.ra.base_spring_boot.model.Album;
import com.ra.base_spring_boot.model.Song;
import com.ra.base_spring_boot.repository.AlbumRepository;
import com.ra.base_spring_boot.repository.IUserRepository;
import com.ra.base_spring_boot.repository.SongRepository;
import com.ra.base_spring_boot.services.album.AlbumService;
import com.ra.base_spring_boot.services.playlist.PlaylistService;
import com.ra.base_spring_boot.services.song.SongService;
import com.ra.base_spring_boot.services.user.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static io.jsonwebtoken.lang.Strings.startsWithIgnoreCase;

@Service
public class GuestServiceImpl implements GuestService {


    private static final String SESSION_KEY = "guest_recent_songs";
    private static final int MAX_SIZE = 10;

    @Autowired
    private SongRepository songRepository;
    @Autowired
    private AlbumRepository albumRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private SongService songService;
    @Autowired
    private AlbumService albumService;
    @Autowired
    private PlaylistService playlistService;

    private SongResponseDTO convertToSongResponse(Song song) {
        Long albumId = null;
        String albumName = null;
        if (song.getAlbums() != null) {
            albumId = song.getAlbums().getId();
            albumName = song.getAlbums().getTitle();
        }
        String artistName = null;
        if (song.getUsers() != null) {
            String firstName = song.getUsers().getFirstName();
            String lastName = song.getUsers().getLastName();
            artistName = (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
        }
        return SongResponseDTO.builder()
                .id(song.getId())
                .title(song.getTitle())
                .duration(song.getDuration())
                .artistId(song.getUsers() != null ? song.getUsers().getId() : 0)
                .artistName(artistName)
                .profilePicUrl(song.getProfilePicUrl())
                .fileUrl(song.getFileUrl())
                .albumId(albumId)
                .albumName(albumName)
                .views(song.getViews())
                .build();
    }

    private TopResultResponseDTO determineTopResult(
            String keyword,
            List<SongResponseDTO> songs,
            List<UserResponseDTO> users,
            List<AlbumResponseDTO> albums,
            List<PlaylistResponseDTO> playlists
    ) {
        if (songs != null) {
            for (SongResponseDTO song : songs) {
                if (startsWithIgnoreCase(song.getTitle(), keyword)) {
                    return new TopResultResponseDTO("Song", song);
                }
            }
        }

        if (users != null) {
            for (UserResponseDTO user : users) {
                String fullName = (user.getFirstName() + " " + user.getLastName()).trim();
                if (startsWithIgnoreCase(fullName, keyword)) {
                    return new TopResultResponseDTO("Artist", user);
                }
            }
        }

        if (albums != null) {
            for (AlbumResponseDTO album : albums) {
                if (startsWithIgnoreCase(album.getTitleAlbum(), keyword)) {
                    return new TopResultResponseDTO("Album", album);
                }
            }
        }

        if (playlists != null) {
            for (PlaylistResponseDTO playlist : playlists) {
                if (startsWithIgnoreCase(playlist.getDescription(), keyword)) {
                    return new TopResultResponseDTO("Playlist", playlist);
                }
            }
        }

        // fallback: lấy kết quả đầu tiên nếu có
        if (!songs.isEmpty()) return new TopResultResponseDTO("Song", songs.get(0));
        if (!users.isEmpty()) return new TopResultResponseDTO("Artist", users.get(0));
        if (!albums.isEmpty()) return new TopResultResponseDTO("Album", albums.get(0));
        if (!playlists.isEmpty()) return new TopResultResponseDTO("Playlist", playlists.get(0));

        return null;
    }


    @Override
    public List<TopArtistResponseDTO> findTop10ArtistsByTotalViews() {
        List<Object[]> results = songRepository.findTop10ArtistsByTotalViews();
        return results.stream().map(row -> {
            Long id = ((Number) row[0]).longValue();
            String firstName = (String) row[1];
            String lastName = (String) row[2];
            String profile_image = (String) row[3];
            BigDecimal totalViews = (BigDecimal) row[4];
            return TopArtistResponseDTO.builder()
                    .artistId(id)
                    .firstName(firstName)
                    .lastName(lastName)
                    .profile_image(profile_image)
                    .totalViews(totalViews)
                    .build();
        }).toList();
    }

    @Override
    public List<TopGenreResponseDTO> findTop10GenresByTotalViews() {
        List<Object[]> results = songRepository.findTop10GenresByTotalViews();
        return results.stream().map(row -> TopGenreResponseDTO.builder()
                .genreId(((Number) row[0]).longValue())
                .genreName((String) row[1])
                .genreImage((String) row[2])
                .totalViews((BigDecimal) row[3])
                .build()).toList();
    }

    @Override
    public List<TopAlbumResponseDTO> findTop10AlbumsByTotalViews() {
        List<Object[]> results = songRepository.findTop10AlbumsBySongViews();
        return results.stream().map(row -> TopAlbumResponseDTO.builder()
                .albumId(((Number) row[0]).longValue())
                .albumName((String) row[1])
                .albumImage((String) row[2])
                .totalViews(((Number) row[3]).longValue())
                .build()).toList();
    }

    @Override
    public List<TopSongOfWeekResponseDTO> findTopSongsOfWeekByTotalViews() {
        List<Song> results = songRepository.findTop15SongsOfWeekByViews();
        return results.stream()
                .map(song->TopSongOfWeekResponseDTO.builder()
                        .songId(song.getId())
                        .songName(song.getTitle())
                        .Views(song.getViews())
                        .fileUrl(song.getFileUrl())
                        .profile_pic_url(song.getProfilePicUrl())
                        .artistName(song.getUsers().getFirstName())
                        .build()).toList();

    }

    @Override
    public void savePlayedSongForGuest(int idSong, HttpSession session) {
        Song song = songRepository.findById((long) idSong);
        if (song == null || song.getStatus() != com.ra.base_spring_boot.model.constants.ApprovalStatus.APPROVED) {
            throw new IllegalArgumentException("Song not found or not approved");
        }
        List<SongResponseDTO> songList = (List<SongResponseDTO>) session.getAttribute(SESSION_KEY);
        if(songList==null) {
            songList = new LinkedList<>();
        }

        songList.add(0, convertToSongResponse(song));
        if (songList.size() > MAX_SIZE) {
            songList = songList.subList(0, MAX_SIZE);
        }

        session.setAttribute(SESSION_KEY, songList);
    }


    @Override
    public List<SongResponseDTO> getRecentlyPlayedSongs(HttpSession session) {
        List<SongResponseDTO> songList = (List<SongResponseDTO>) session.getAttribute(SESSION_KEY);
        return songList != null ? songList : Collections.emptyList();
    }




    @Override
    public List<GuestAlbumResponseDTO> getAllAlbums() {
        List<Album> albums = albumRepository.findAllApprovedAlbums();
        return albums.stream().map(album -> GuestAlbumResponseDTO.builder()
                .id(album.getId())
                .title(album.getTitle())
                .coverUrl(album.getCoverImage())
                .artistName(getArtistFullNameFromAlbum(album))
                .build()).toList();
    }

    @Override
    public List<FeaturedAlbumResponseDTO> getFeaturedAlbums() {
        List<Album> albums = albumRepository.findFeaturedAlbums();
        return albums.stream().map(a -> {
            List<Song> songs = songRepository.findApprovedSongsByAlbumId(a.getId());
            long totalViews = songs.stream().mapToLong(Song::getViews).sum();
            return FeaturedAlbumResponseDTO.builder()
                    .id(a.getId())
                    .title(a.getTitle())
                    .coverUrl(a.getCoverImage())
                    .artistName(getArtistFullNameFromAlbum(a))
                    .totalViews(totalViews)
                    .build();
        }).toList();
    }

    @Override
    public List<Top15AlbumsResponseDTO> getTop15Albums() {
        List<Album> albums = albumRepository.findTop15Albums();
        return albums.stream().map(a -> {
            List<Song> songs = songRepository.findApprovedSongsByAlbumId(a.getId());
            long totalViews = songs.stream().mapToLong(Song::getViews).sum();
            return Top15AlbumsResponseDTO.builder()
                    .id(a.getId())
                    .title(a.getTitle())
                    .coverUrl(a.getCoverImage())
                    .artistName(getArtistFullNameFromAlbum(a))
                    .totalViews(totalViews)
                    .build();
        }).toList();
    }

    @Override
    public List<GuestAlbumResponseDTO> getAlbumsByArtist(Long artistId) {
        List<Album> albums = albumRepository.findAlbumsByArtistId(artistId);

        if (albums.isEmpty()) {
            return Collections.emptyList();
        }

        return albums.stream()
                .map(album -> GuestAlbumResponseDTO.builder()
                        .id(album.getId())
                        .title(album.getTitle())
                        .coverUrl(album.getCoverImage())
                        .artistName(getArtistFullNameFromAlbum(album))
                        .createdAtStr(album.getCreatedAt() != null ?
                                album.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) : null)
                        .build())
                .toList();
    }

    @Override
    public List<GuestAlbumResponseDTO> getOneTopAlbumPerArtist() {
        List<Album> albums = albumRepository.findOneTopAlbumPerArtist();
        return albums.stream().map(album -> GuestAlbumResponseDTO.builder()
                .id(album.getId())
                .title(album.getTitle())
                .coverUrl(album.getCoverImage())
                .artistName(getArtistFullNameFromAlbum(album))
                .build()).toList();
    }

    @Override
    public List<GuestAlbumResponseDTO> getNewlyReleasedAlbums() {
        List<Album> albums = albumRepository.findNewlyReleasedApprovedAlbums();
        return albums.stream().map(album -> GuestAlbumResponseDTO.builder()
                .id(album.getId())
                .title(album.getTitle())
                .coverUrl(album.getCoverImage())
                .artistName(getArtistFullNameFromAlbum(album))
                .createdAtStr(album.getCreatedAt() != null ? album.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) : null)
                .build()).toList();
    }

    @Override
    public List<SongResponseDTO> findTopSongsByTotalViews() {
        List<Song> songs = songRepository.findTopSongByViews();
        return songs.stream()
                .map(this::convertToSongResponse)
                .toList();
    }

    private String getArtistFullNameFromAlbum(Album album) {
        if (album.getUsers() == null) return null;
        String firstName = album.getUsers().getFirstName();
        String lastName = album.getUsers().getLastName();
        return (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
    }
    @Override
    public List<SongResponseDTO> getTopSongsInMonth() {
        List<Song> songs = songRepository.getTopSongsInMonth();
        return songs.stream().map(song -> convertToSongResponse(song)).toList();
    }
    @Override
    public FullTextSearchResponseDTO getFullTextSearchResponseDTO(String keyword) {
        String lowerKeyword = keyword.toLowerCase();

        List<SongResponseDTO> songList = songService.fullTextSearchSong(keyword);
        List<UserResponseDTO> userList = userService.fullTextSearchUsers(keyword);
        List<AlbumResponseDTO> albumList = albumService.fullTextSearchAlbums(keyword);
        List<PlaylistResponseDTO> playlistList = playlistService.fullTextSearchPlaylist(keyword);

        TopResultResponseDTO topResult = determineTopResult(lowerKeyword, songList, userList, albumList, playlistList);

        return FullTextSearchResponseDTO.builder()
                .topResult(topResult)
                .songList(songList)
                .userList(userList)
                .albumList(albumList)
                .playlistList(playlistList)
                .build();
    }

}
