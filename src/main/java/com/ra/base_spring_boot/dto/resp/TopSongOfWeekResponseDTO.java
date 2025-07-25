package com.ra.base_spring_boot.dto.resp;

import com.ra.base_spring_boot.model.Album;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class TopSongOfWeekResponseDTO {
    private Long songId;
    private String songName;
    private String artistName;
    private String profile_pic_url;
    private String fileUrl;
    private long Views;

}
