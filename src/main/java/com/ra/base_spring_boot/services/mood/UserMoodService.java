package com.ra.base_spring_boot.services.mood;

import com.ra.base_spring_boot.model.User;
import com.ra.base_spring_boot.model.UserMood;

public interface UserMoodService {
    boolean hasTodayMood(Long userId);
    UserMood getUserMood(User user, String userText);
}
