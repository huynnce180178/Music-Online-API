package com.ra.base_spring_boot.dto.resp;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuestAlbumResponseDTO {
    private Long id;
    private String title;
    private String coverUrl;
    private String artistName;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String createdAtStr;

}
