package com.ra.base_spring_boot.model;

import com.ra.base_spring_boot.model.base.BaseObject;

import com.ra.base_spring_boot.model.constants.AgeGroup;
import com.ra.base_spring_boot.model.constants.MoodType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "song_profiles")
public class SongProfile extends BaseObject {

    @Enumerated(EnumType.STRING)
    @Column(name = "mood_type", nullable = false, length = 50)
    private MoodType moodType;

    @Enumerated(EnumType.STRING)
    @Column(name = "age_group", nullable = false, length = 50)
    private AgeGroup ageGroup;

    @OneToOne
    @JoinColumn(name = "song_id", nullable = false, unique = true)
    private Song song;
}

