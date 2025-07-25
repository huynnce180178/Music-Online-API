package com.ra.base_spring_boot.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SongDetailDTO {
    private SongResponseDTO song;
    private ArtistResponseDTO artist;
    private AlbumResponseDTO album;
    private List<CommentResponseDTO> comments;
}
