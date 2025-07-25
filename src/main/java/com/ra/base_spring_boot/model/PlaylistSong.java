package com.ra.base_spring_boot.model;

import com.ra.base_spring_boot.model.base.BaseObject;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "playlist_song")
public class PlaylistSong extends BaseObject {
    @ManyToOne
    @JoinColumn(name = "playlist_id", referencedColumnName = "id", nullable = false)
    private Playlist playlists;

    @ManyToOne
    @JoinColumn(name = "song_id", referencedColumnName = "id", nullable = false)
    private Song songs;

    @Column(name = "added_at", nullable = false)
    private LocalDateTime addedAt;
}