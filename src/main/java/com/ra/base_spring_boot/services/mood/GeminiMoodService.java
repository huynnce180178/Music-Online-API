package com.ra.base_spring_boot.services.mood;


import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.ra.base_spring_boot.model.Playlist;
import com.ra.base_spring_boot.model.PlaylistSong;
import com.ra.base_spring_boot.model.Song;
import com.ra.base_spring_boot.model.SongProfile;
import com.ra.base_spring_boot.model.constants.AgeGroup;
import com.ra.base_spring_boot.model.constants.MoodType;
import com.ra.base_spring_boot.repository.PlaylistRepository;
import com.ra.base_spring_boot.repository.PlaylistSongRepository;
import com.ra.base_spring_boot.repository.SongProfileRepository;
import com.ra.base_spring_boot.repository.SongRepository;
import com.ra.base_spring_boot.services.song.SongService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GeminiMoodService {
    @Value("${gemini.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate;

    @Autowired
    private SongService songService;
    @Autowired
    private SongProfileRepository songProfileRepository;
    @Autowired
    private SongRepository songRepository;
    @Autowired
    private PlaylistRepository playlistRepository;
    @Autowired
    private PlaylistSongRepository playlistSongRepository;

    private MoodType fromString(String moodType) {

        if (moodType == null || moodType.isEmpty()) {
            return MoodType.NEUTRAL;
        }
        String normalized = moodType.trim().toUpperCase();
        for (MoodType type : MoodType.values()) {
            if (type.toString().equals(normalized)) {
                return type;
            }
        }
        return MoodType.NEUTRAL;
    }

    ;


    public String detectMood(String userInput) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent?key=" + apiKey;

        Map<String, Object> requestBody = Map.of("contents", new Object[]{
                        Map.of("parts", new Object[]{
                                Map.of("text",
                                        "You are a sentiment classifier. Return only ONE word from the following list: HAPPY, SAD, NEUTRAL. No explanation. Only return one word in uppercase.\n" +
                                                "Text: \"" + userInput + "\"\n" +
                                                "Answer:"
                                )
                        })
                }
        );
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

            String output = response.getBody();
            System.out.println(">>> Gemini full raw output:\n" + output);
            JsonObject root = JsonParser.parseString(output).getAsJsonObject();

            JsonArray candidates = root.getAsJsonArray("candidates");
            if (candidates.size() > 0 && candidates != null) {
                JsonObject content = candidates.get(0).getAsJsonObject().getAsJsonObject("content");
                JsonArray parts = content.getAsJsonArray("parts");
                if (parts != null && parts.size() > 0) {
                    String mood = parts.get(0).getAsJsonObject().get("text").getAsString().trim().toUpperCase();
                    return mood;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "NEUTRAL";
        }
        return "NEUTRAL";
    }

    public void checkMoodSong(Long songId) {
        if (songProfileRepository.existsBySongId(songId)) {
            return;
        }
        String songLyrics = songRepository.getById(songId).getLyrics();
        if (songLyrics == null || songLyrics.trim().isEmpty()) {
            System.err.println("❌ Song has no lyrics. Skipping mood and age group detection.");
            return;
        }
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent?key=" + apiKey;
        String moodList = Arrays.stream(MoodType.values())
                .map(Enum::name)
                .collect(Collectors.joining(", "));

        // Lấy danh sách các nhóm tuổi dưới dạng một chuỗi, được phân tách bằng dấu phẩy
        String ageGroupList = Arrays.stream(AgeGroup.values())
                .map(Enum::name)
                .collect(Collectors.joining(", "));

        // Xây dựng lời nhắc chi tiết để hướng dẫn mô hình
        String prompt = String.format(
                "You are a music classifier. Your only task is to determine the MOOD and AGE_GROUP for given song lyrics and respond in the specified format.\n\n" +
                        "Available moods: %s\n" +
                        "Available age groups: %s\n\n" +
                        "--- EXAMPLE START ---\n" +
                        "Lyrics: \"I'm walking on sunshine, whoa-oh! And don't it feel good!\"\n" +
                        "MOOD: HAPPY\n" +
                        "AGE_GROUP: TEENS\n" +
                        "--- EXAMPLE END ---\n\n" +
                        "Now, complete the following based on the lyrics provided below.\n\n" +
                        "Lyrics: \"%s\"\n\n" +
                        "Your response MUST contain ONLY the 2 lines below, without any other text, explanations, or formatting:\n" +
                        "MOOD: [ONE MOOD IN UPPERCASE]\n" +
                        "AGE_GROUP: [ONE AGE GROUP IN UPPERCASE]",
                moodList, ageGroupList, songLyrics
        );


        // Tạo phần thân yêu cầu (request body) cho API
        Map<String, Object> requestBody = Map.of("contents", new Object[]{
                Map.of("parts", new Object[]{
                        Map.of("text", prompt)
                })
        });
        //return answer is: sad 17, happy 32 ....
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

            String output = response.getBody();
            System.out.println(">>> Gemini full raw output:\n" + output);
            JsonObject root = JsonParser.parseString(output).getAsJsonObject();

            JsonArray candidates = root.getAsJsonArray("candidates");
            if (candidates.size() > 0 && candidates != null) {
                JsonObject content = candidates.get(0).getAsJsonObject().getAsJsonObject("content");
                JsonArray parts = content.getAsJsonArray("parts");
                if (parts != null && parts.size() > 0) {
                    String rawApiResponse = parts.get(0).getAsJsonObject().get("text").getAsString();
                    System.out.println("📥 Gemini response:\n" + rawApiResponse);

                    String[] lines = rawApiResponse.trim().split("\n");
                    if (lines.length < 2 || !lines[0].startsWith("MOOD:") || !lines[1].startsWith("AGE_GROUP:")) {
                        System.err.println("❌ Invalid Gemini format:\n" + rawApiResponse);
                        return;
                    }

                    String moodString = lines[0].substring("MOOD: ".length()).trim().toUpperCase();
                    String ageGroupString = lines[1].substring("AGE_GROUP: ".length()).trim().toUpperCase();

                    MoodType detectedMood = MoodType.valueOf(moodString);
                    AgeGroup detectedAgeGroup = AgeGroup.valueOf(ageGroupString);

                    SongProfile newSongProfile = SongProfile.builder()
                            .song(songRepository.getById(songId))
                            .moodType(detectedMood)
                            .ageGroup(detectedAgeGroup)
                            .build();

                    songProfileRepository.save(newSongProfile);
                    System.out.println("✅ SongProfile saved: " + detectedMood + " - " + detectedAgeGroup);

                    try {
                        // Get playlist name for age group (same logic as PlaylistServiceImpl)
                        String playlistName = switch (detectedAgeGroup) {
                            case TEENAGER -> "Teenify";
                            case YOUNG_ADULT -> "Youngify";
                            case ADULT -> "Adultify";
                            case MIDDLE_AGE -> "Midify";
                            case MATURE_ADULT -> "Maturify";
                            case OLDER_ADULT -> "Oldify";
                            case SENIOR -> "Goldify";
                        };
                        Playlist playlist = playlistRepository.findByName(playlistName)
                            .orElseThrow(() -> new RuntimeException("Playlist not found for age group: " + detectedAgeGroup));
                        Song song = songRepository.getById(songId);
                        boolean exists = playlistSongRepository.findByPlaylists_IdAndSongs_Id(playlist.getId(), songId).isPresent();
                        if (!exists) {
                            PlaylistSong ps = PlaylistSong.builder()
                                .playlists(playlist)
                                .songs(song)
                                .addedAt(java.time.LocalDateTime.now())
                                .build();
                            playlistSongRepository.save(ps);
                            System.out.println("✅ Song added to playlist: " + playlistName);
                        }
                    } catch (Exception ex) {
                        System.err.println("❌ Failed to add song to age group playlist: " + ex.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Lỗi khi phân tích phản hồi từ Gemini API:");
        }


    }
}
