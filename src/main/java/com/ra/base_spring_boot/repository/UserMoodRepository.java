package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.model.User;
import com.ra.base_spring_boot.model.UserMood;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface UserMoodRepository extends JpaRepository<UserMood, Long> {
    Optional<UserMood> findByUserAndMoodDate(User user, LocalDate moodDate);
    Optional<UserMood> findByUser(User user);

//    Optional<UserMood> findFirstByUserOrderByCreatedAtDesc(User user);
//
//    List<UserMood> findByUserOrderByCreatedAtDesc(User user);
//
//    @Query("SELECT um FROM UserMood um WHERE um.user = :user AND DATE(um.createdAt) = DATE(:date)")
//    Optional<UserMood> findByUserAndDate(@Param("user") User user, @Param("date") LocalDateTime date);
//
//    List<UserMood> findByMoodTypeOrderByCreatedAtDesc(MoodType moodType);
//
//    boolean existsByMoodType(MoodType moodType);
//
//    void deleteByMoodType(MoodType moodType);
//
//    // Đếm số lượng user duy nhất theo mood type
//    @Query("SELECT COUNT(DISTINCT um.user) FROM UserMood um WHERE um.moodType = :moodType")
//    Long countDistinctUsersByMoodType(@Param("moodType") MoodType moodType);
//
//    // Lấy mood mới nhất của mỗi user theo mood type
//    @Query("SELECT um FROM UserMood um WHERE um.moodType = :moodType AND um.createdAt = " +
//            "(SELECT MAX(um2.createdAt) FROM UserMood um2 WHERE um2.user = um.user)")
//    List<UserMood> findLatestUserMoodsByMoodType(@Param("moodType") MoodType moodType);
//
//    // Lấy tất cả mood types với số lượng user
//    @Query("SELECT um.moodType as moodType, COUNT(DISTINCT um.user) as userCount " +
//            "FROM UserMood um GROUP BY um.moodType ORDER BY userCount DESC")
//    List<Object[]> findMoodTypeStatsWithUserCount();
//
//    // Thống kê số lượng user theo từng mức độ (intensity) của một loại tâm trạng
//    @Query("SELECT um.intensity, COUNT(DISTINCT um.user.id) FROM UserMood um WHERE um.moodType = :moodType GROUP BY um.intensity ORDER BY um.intensity")
//    List<Object[]> countUsersByIntensityForMoodType(@Param("moodType") MoodType moodType);
}