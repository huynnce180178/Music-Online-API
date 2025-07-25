package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.model.Genre;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface GenreRepository extends JpaRepository<Genre, Long> {
    Page<Genre> findByGenreNameContainingIgnoreCase(String genreName, Pageable pageable);

    @Query("SELECT g FROM Genre g")
    Page<Genre> findAllGenreNames(Pageable pageable);
}