package com.ra.base_spring_boot.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
@Getter
@Setter
@AllArgsConstructor
@Builder
public class GenreWithSongResponseDTO {
    private Long genreId;
    private String genreName;
    private List<SongWithGenreReponseDTO> songs;
}
