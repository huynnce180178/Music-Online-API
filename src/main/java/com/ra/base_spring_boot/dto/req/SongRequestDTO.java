package com.ra.base_spring_boot.dto.req;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class SongRequestDTO {
    private String title;
    private MultipartFile file;
    private MultipartFile profilePic;
    private List<Long> genreIds;
    private String lyrics;

}
