package com.ra.base_spring_boot.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Time;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class SongHistoryResponseDTO {
    private Long historyId;
    private Long songId;
    private String albumName;
    private String title;
    private Time duration;
    private String artistName;
    private String fileUrl;
    private String profilePicUrl;
    private Long views;
    private String playedAtStr;

}
