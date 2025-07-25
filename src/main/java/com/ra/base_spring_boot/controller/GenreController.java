package com.ra.base_spring_boot.controller;

import com.ra.base_spring_boot.dto.req.GenreRequestDTO;
import com.ra.base_spring_boot.dto.resp.GenreResponseDTO;
import com.ra.base_spring_boot.services.genre.GenreService;
import com.ra.base_spring_boot.dto.ResponseWrapper; // Import ResponseWrapper
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name="Genre Controller")
@RestController
@RequestMapping("/api/v1")
public class GenreController {

    @Autowired
    private GenreService genreService;

    @Operation(summary = "Lấy tất cả các thể loại", description = "Lấy danh sách tất cả các thể loại trong hệ thống")    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved genres"),
            @ApiResponse(responseCode = "404", description = "Genres not found")
    })
    @GetMapping("/genres")
    public ResponseEntity<ResponseWrapper<?>> getAllGenres(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of genres per page") @RequestParam(defaultValue = "5") int size,
            @Parameter(description = "Field to sort by") @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Sort direction: asc or desc") @RequestParam(defaultValue = "asc") String sortDir,
            @Parameter(description = "Search term for genre name") @RequestParam(required = false) String search
    ) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<GenreResponseDTO> genres = genreService.getAllGenres(pageable, search);
        return ResponseEntity.ok(
                ResponseWrapper.builder()
                        .data(genres.getContent())
                        .code(HttpStatus.OK.value())
                        .status(HttpStatus.OK)
                        .build()
        );
    }

    @Operation(summary = "Tạo thể loại mới", description = "Tạo thể loại mới trong hệ thống")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Successfully created genre")
    })
    @PostMapping("/admin/genres/add")
    public ResponseEntity<ResponseWrapper<?>> createGenre(@ModelAttribute GenreRequestDTO genreRequestDTO) {
        GenreResponseDTO created = genreService.createGenre(genreRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ResponseWrapper.builder()
                        .data(created)
                        .code(HttpStatus.CREATED.value())
                        .status(HttpStatus.CREATED)
                        .build()
        );
    }

    @Operation(summary = "Cập nhật thể loại hiện có", description = "Cập nhật thông tin chi tiết của thể loại hiện có")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully updated genre"),
            @ApiResponse(responseCode = "404", description = "Genre not found")
    })
    @PutMapping("/admin/genre/update/{id}")
    public ResponseEntity<ResponseWrapper<?>> updateGenre(@PathVariable Long id, @ModelAttribute GenreRequestDTO genreRequestDTO) {
        GenreResponseDTO updated = genreService.updateGenre(id, genreRequestDTO);
        return ResponseEntity.ok(
                ResponseWrapper.builder()
                        .data(updated)
                        .code(HttpStatus.OK.value())
                        .status(HttpStatus.OK)
                        .build()
        );
    }

    @Operation(summary = "Xóa một thể loại", description = "Xóa một thể loại khỏi hệ thống theo ID của thể loại đó")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Successfully deleted genre"),
            @ApiResponse(responseCode = "404", description = "Genre not found")
    })
    @DeleteMapping("/admin/genre/delete/{id}")
    public ResponseEntity<ResponseWrapper<?>> deleteGenre(@PathVariable Long id) {
        genreService.deleteGenre(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(
                ResponseWrapper.builder()
                        .data("Genre deleted successfully")
                        .code(HttpStatus.NO_CONTENT.value())
                        .status(HttpStatus.NO_CONTENT)
                        .build()
        );
    }
}