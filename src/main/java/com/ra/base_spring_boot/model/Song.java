package com.ra.base_spring_boot.model;

import com.ra.base_spring_boot.model.base.BaseObject;
import com.ra.base_spring_boot.model.constants.ApprovalStatus;
import jakarta.persistence.*;
import lombok.*;

import java.sql.Time;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "songs")
public class Song extends BaseObject {
    @Column(name = "title", length = 255, nullable = false)
    private String title;

    @Column(name = "duration", nullable = false)
    private Time duration;

    @ManyToOne
    @JoinColumn(name = "artist_id", referencedColumnName = "id", nullable = false)
    private User users;

    @ManyToOne
    @JoinColumn(name = "album_id", referencedColumnName = "id", nullable = true)
    private Album albums;

    @Column(name = "file_url", length = 255)
    private String fileUrl;

    @Column(name = "profile_pic_url", length = 255)
    private String profilePicUrl;

    @Column(name = "views")
    private Long views;

    @Column(name = "lyrics", columnDefinition = "TEXT")
    private String lyrics;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private ApprovalStatus status = ApprovalStatus.PENDING;

    @ManyToMany
    @JoinTable(
            name = "song_genre",
            joinColumns = @JoinColumn(name = "song_id"),
            inverseJoinColumns = @JoinColumn(name = "genre_id")
    )
    private List<Genre> genres;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
