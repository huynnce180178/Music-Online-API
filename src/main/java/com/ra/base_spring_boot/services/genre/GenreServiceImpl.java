package com.ra.base_spring_boot.services.genre;

import com.ra.base_spring_boot.dto.req.GenreRequestDTO;
import com.ra.base_spring_boot.dto.resp.GenreResponseDTO;
import com.ra.base_spring_boot.dto.resp.GenreWithSongResponseDTO;
import com.ra.base_spring_boot.dto.resp.GuestGenreResponseDTO;
import com.ra.base_spring_boot.dto.resp.SongWithGenreReponseDTO;
import com.ra.base_spring_boot.model.Genre;
import com.ra.base_spring_boot.repository.GenreRepository;
import com.ra.base_spring_boot.repository.SongRepository;
import com.ra.base_spring_boot.services.impl.CloudinaryService;
import com.ra.base_spring_boot.services.song.SongService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class GenreServiceImpl implements GenreService {


    @Autowired
    private GenreRepository genreRepository;
    @Autowired
    private CloudinaryService cloudinaryService;
    @Autowired
    private SongRepository songRepository;


    @Override
    public GenreResponseDTO createGenre(GenreRequestDTO genreRequestDTO) {
        Genre genre = new Genre();
        genre.setGenreName(genreRequestDTO.getGenreName());
        genre.setGenreImage(cloudinaryService.uploadFile(genreRequestDTO.getGenreImage(), "image"));
        Genre saved = genreRepository.save(genre);
        return new GenreResponseDTO(
                saved.getId(),
                saved.getGenreName(),
                saved.getGenreImage());
    }

    @Override
    public GenreResponseDTO updateGenre(Long id, GenreRequestDTO genreRequestDTO) {
        Genre genre = genreRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Genre not found"));
        genre.setGenreName(genreRequestDTO.getGenreName());
        genre.setGenreImage(cloudinaryService.uploadFile(genreRequestDTO.getGenreImage(), "image"));
        Genre updated = genreRepository.save(genre);
        return new GenreResponseDTO(
                updated.getId(),
                updated.getGenreName(),
                updated.getGenreImage());
    }

    @Override
    public void deleteGenre(Long id) {
        genreRepository.deleteById(id);
    }

    @Override
    public GenreResponseDTO getGenreById(Long id) {
        Genre genre = genreRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Genre not found"));
        return new GenreResponseDTO(
                genre.getId(),
                genre.getGenreName(),
                genre.getGenreImage());
    }

    @Override
    public Page<GenreResponseDTO> getAllGenres(Pageable pageable, String search) {
        Page<Genre> genres;
        if (search != null && !search.isEmpty()) {
            genres = genreRepository.findByGenreNameContainingIgnoreCase(search, pageable);
        } else {
            genres = genreRepository.findAll(pageable);
        }
        return genres.map(g -> new GenreResponseDTO(g.getId(),g.getGenreName(),g.getGenreImage()));
    }
    @Override
    public Page<GenreWithSongResponseDTO> getGenresWithSongs(Pageable pageable) {
        Page<Genre> genres = genreRepository.findAllGenreNames(pageable);

        List<GenreWithSongResponseDTO> dtoList = genres.getContent().stream()
                .map(genre -> {
                    List<Object[]> rawSongs = songRepository.findSongsByGenreName(genre.getGenreName());

                    List<SongWithGenreReponseDTO> songs = rawSongs.stream()
                            .map(row -> {
                                try {
                                    return SongWithGenreReponseDTO.builder()
                                            .id((Long) row[0])
                                            .title((String) row[1])
                                            .fileUrl((String) row[2])
                                            .profilePicUrl((String) row[3])
                                            .build();
                                } catch (ClassCastException e) {
                                    throw new IllegalStateException("Failed to process song data: " + e.getMessage());
                                }
                            })
                            // Remove duplicate songs by id
                            .collect(Collectors.collectingAndThen(
                                Collectors.toMap(SongWithGenreReponseDTO::getId, s -> s, (s1, s2) -> s1),
                                m -> m.values().stream().collect(Collectors.toList())
                            ));

                    return new GenreWithSongResponseDTO(genre.getId(), genre.getGenreName(), songs);
                })
                .collect(Collectors.toList());

        return new PageImpl<>(dtoList, pageable, genres.getTotalElements());
    }
    @Override
    public Long genreCount() {
        return genreRepository.count();
    }

    @Override
    public List<SongWithGenreReponseDTO> getSongsByGenreId(Long genreId) {
        genreRepository.findById(genreId)
                .orElseThrow(() -> new IllegalArgumentException("Genre not found"));

        List<Object[]> rawSongs = songRepository.findSongsByGenreId(genreId);

        return rawSongs.stream()
                .map(row -> SongWithGenreReponseDTO.builder()
                        .id((Long) row[0])
                        .title((String) row[1])
                        .fileUrl((String) row[2])
                        .profilePicUrl((String) row[3])
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<GuestGenreResponseDTO> getAllGenres() {
        return genreRepository.findAll().stream()
                .map(genre -> GuestGenreResponseDTO.builder()
                        .id(genre.getId())
                        .name(genre.getGenreName())
                        .image(genre.getGenreImage())
                        .build())
                .collect(Collectors.toList());
    }
}
