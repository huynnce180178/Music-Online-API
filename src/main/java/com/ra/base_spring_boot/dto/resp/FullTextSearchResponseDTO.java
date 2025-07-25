package com.ra.base_spring_boot.dto.resp;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
@Getter
@Setter
@Builder
public class FullTextSearchResponseDTO {
    private TopResultResponseDTO topResult;
    private List<SongResponseDTO> songList;
    private List<UserResponseDTO> userList;
    private List<AlbumResponseDTO> albumList;
    private List<PlaylistResponseDTO> playlistList;
}
