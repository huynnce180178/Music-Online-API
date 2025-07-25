package com.ra.base_spring_boot.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class StatisticUserResponseDTO {
    private Long totalUser;
    private Long userRoleUser;
    private Long userRoleArtist;
    private Long userRolePremium;
}
