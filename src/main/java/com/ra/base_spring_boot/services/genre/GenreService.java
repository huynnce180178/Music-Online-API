package com.ra.base_spring_boot.services.genre;

import com.ra.base_spring_boot.dto.req.GenreRequestDTO;
import com.ra.base_spring_boot.dto.resp.GenreResponseDTO;
import com.ra.base_spring_boot.dto.resp.GenreWithSongResponseDTO;
import com.ra.base_spring_boot.dto.resp.GuestGenreResponseDTO;
import com.ra.base_spring_boot.dto.resp.SongWithGenreReponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface GenreService {
    GenreResponseDTO createGenre(GenreRequestDTO genreRequestDTO);
    GenreResponseDTO updateGenre(Long id, GenreRequestDTO genreRequestDTO);
    void deleteGenre(Long id);
    GenreResponseDTO getGenreById(Long id);
    Page<GenreResponseDTO> getAllGenres(Pageable pageable, String search);
    Page<GenreWithSongResponseDTO> getGenresWithSongs(Pageable pageable);
    Long genreCount();
    List<SongWithGenreReponseDTO> getSongsByGenreId(Long genreId);
    List<GuestGenreResponseDTO> getAllGenres();
}
