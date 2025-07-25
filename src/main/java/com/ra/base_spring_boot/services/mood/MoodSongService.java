package com.ra.base_spring_boot.services.mood;

import com.ra.base_spring_boot.model.SongProfile;

public interface MoodSongService {
    SongProfile waitUntilMoodExists(Long songId, int timeoutMillis);
}
