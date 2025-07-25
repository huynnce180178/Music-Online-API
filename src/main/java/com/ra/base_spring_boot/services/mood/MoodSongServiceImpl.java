package com.ra.base_spring_boot.services.mood;


import com.ra.base_spring_boot.model.SongProfile;
import com.ra.base_spring_boot.repository.SongProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MoodSongServiceImpl implements MoodSongService {
    @Autowired
    private SongProfileRepository songProfileRepository;

    @Override
    public SongProfile waitUntilMoodExists(Long songId, int timeoutMillis) {
        int elapsed = 0, interval = 200;
        while (elapsed < timeoutMillis) {
            SongProfile moodSong = songProfileRepository.findBySongId(songId);
            if (moodSong != null) {
                return moodSong;
            }
            try {
                Thread.sleep(interval);
            } catch (InterruptedException ignored) {}
            elapsed += interval;
        }
        return null;
    }
}
