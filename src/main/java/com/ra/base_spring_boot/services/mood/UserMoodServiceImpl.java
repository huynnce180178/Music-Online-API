package com.ra.base_spring_boot.services.mood;

import com.ra.base_spring_boot.exception.HttpNotFound;
import com.ra.base_spring_boot.model.User;
import com.ra.base_spring_boot.model.UserMood;
import com.ra.base_spring_boot.model.constants.MoodType;
import com.ra.base_spring_boot.repository.IUserRepository;
import com.ra.base_spring_boot.repository.UserMoodRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class UserMoodServiceImpl implements UserMoodService {


    @Autowired
    private UserMoodRepository userMoodRepository;
    @Autowired
    private IUserRepository userRepository;
    @Autowired
    private GeminiMoodService geminiMoodService;

    private MoodType fromString(String moodType){

        if(moodType  == null || moodType .isEmpty()){
            return MoodType.NEUTRAL;
        }
        String normalized  = moodType.trim().toUpperCase();
        for(MoodType type : MoodType.values()){
            if(type.toString().equals(normalized )){
                return type;
            }
        }
        return MoodType.NEUTRAL;
    };

    @Override
    public boolean hasTodayMood(Long userId) {
        User user = userRepository.getById(userId);
        if (user == null){
            throw new HttpNotFound("User not found");
        }
        return userMoodRepository.findByUser(user)
                .map(mood -> mood.getMoodDate().isEqual(LocalDate.now()))
                .orElse(false);
    }
    @Override
    public UserMood getUserMood(User user, String userText) {
        LocalDate today = LocalDate.now();
        return userMoodRepository.findByUser(user)
                .map(existing -> {
                    String moodText = geminiMoodService.detectMood(userText);
                    MoodType newMood = fromString(moodText);
                    if (!existing.getMoodDate().isEqual(today) || !existing.getMoodType().equals(newMood)) {
                        // Đã có nhưng là ngày cũ hoặc mood mới khác với DB → cập nhật

                        existing.setMoodType(newMood);
                        existing.setUpdatedAt(LocalDateTime.now());
                        existing.setMoodDate(today);
                        return userMoodRepository.save(existing);
                    }
                    return existing; // hôm nay đã có
                })
                .orElseGet(() -> {
                    // Chưa có → tạo mới
                    String moodText = geminiMoodService.detectMood(userText);
                    MoodType mood = fromString(moodText);

                    UserMood newMood = UserMood.builder()
                            .user(user)
                            .moodType(mood)
                            .moodDate(today)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
                    return userMoodRepository.save(newMood);
                });
    }
}
