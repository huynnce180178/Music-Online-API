package com.ra.base_spring_boot.services.mood;

import com.ra.base_spring_boot.dto.req.PlaylistRequestDTO;
import com.ra.base_spring_boot.exception.HttpBadRequest;
import com.ra.base_spring_boot.exception.HttpNotFound;
import com.ra.base_spring_boot.model.*;
import com.ra.base_spring_boot.model.constants.ApprovalStatus;
import com.ra.base_spring_boot.repository.*;
import com.ra.base_spring_boot.services.playlist.PlaylistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class SongSuggestService {
    @Autowired
    private PlaylistService playlistService;
    @Autowired
    private PlaylistRepository playlistRepository;
    @Autowired
    private SongRepository songRepository;
    @Autowired
    private PlaylistSongRepository playlistSongRepository;
//    @Autowired
//    private MoodKeywordService moodKeywordService;
    @Autowired
    private IUserRepository userRepository;
    @Autowired
    private UserMoodService userMoodService;
    @Autowired
    private SongProfileRepository songProfileRepository;
    @Autowired
    private MoodSongService moodSongService;
    @Autowired
    private GeminiMoodService geminiMoodService;

    public void suggestMusicByMood(Long userId, String userText){
        User user = userRepository.getOne(userId);
        if(user == null){
            throw new HttpNotFound("User not found with ID: " + userId);
        }
        UserMood userMood = userMoodService.getUserMood(user, userText);


        Playlist playlist = playlistRepository.findByNameAndUser("Moodify", user);
        if(playlist == null){
            PlaylistRequestDTO playlistRequestDTO = PlaylistRequestDTO.builder()
                    .name("Moodify")
                    .description("Let your mood take the lead.\n" +
                            "Moodify is your daily soundtrack tailored to how you feel. Whether you're floating in joy, drowning in thoughts, or simply cruising through the day — we've got the tunes that match your vibe.\n" +
                            "\n" +
                            "Updated daily. Powered by your emotions.\n" +
                            "\n" +
                            "Feel it. Live it. Moodify it.")
                    .isPublic(false)
                    .build();
            playlistService.createPlaylist(playlistRequestDTO);
            playlist = playlistRepository.findByNameAndUser("Moodify", user);
        }else{
            Long playlistId = playlist.getId();
            playlistSongRepository.deleteAllByPlaylistId(playlistId);
        }
        Long playlistId = playlist.getId();
        Set<Long> addedSongIds = new HashSet<>();
        for(Song song : songRepository.findAll()){
            if(!songProfileRepository.existsBySongId(song.getId()) && song.getLyrics() != null) {
                geminiMoodService.checkMoodSong(song.getId());
            }

            SongProfile moodSong = moodSongService.waitUntilMoodExists(song.getId(), 3000);
            if(moodSong != null && moodSong.getMoodType() == userMood.getMoodType()
                    && song.getStatus() == ApprovalStatus.APPROVED
                    && addedSongIds.add(song.getId())){
                try {
                    playlistService.addSongToPlaylist(playlist.getId(), song.getId());
                } catch (HttpBadRequest ignored) {

                }
            }

        }


//        for(MoodKeyword moodKeyword : moodKeywords){
//            List<Song> matchedSongs = songRepository.findByLyricsContainingIgnoreCase(moodKeyword.getKeyword());
//            for(Song song : matchedSongs){
//                if(song.getStatus() == ApprovalStatus.APPROVED && addedSongIds.add(song.getId())){
//                    try {
//                        playlistService.addSongToPlaylist(playlist.getId(), song.getId());
//                    } catch (HttpBadRequest ignored) {
//
//                    }
//                }
//
//            }
//        }

    }

}
