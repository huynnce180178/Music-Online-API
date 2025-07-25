package com.ra.base_spring_boot.dto.resp;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Time;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class SongResponseDTO {

    private Long id;
    private String title;
    private Time duration;
    private Long artistId;
    private String artistName;
    private Long albumId;
    private String albumName;
    private String fileUrl;
    private String profilePicUrl;
    private Long views;
    private String status;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String createdAtStr;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String updatedAtStr;
}
