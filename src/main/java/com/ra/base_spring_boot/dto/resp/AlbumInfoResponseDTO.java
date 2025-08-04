package com.ra.base_spring_boot.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlbumInfoResponseDTO {
    private AlbumDetailResponseDTO albumDetailResponseDTO;
    private List<SongResponseDTO> songResponseDTO;
}
