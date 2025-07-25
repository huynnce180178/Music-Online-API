package com.ra.base_spring_boot.dto.resp;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ra.base_spring_boot.model.constants.UserStatus;
import com.ra.base_spring_boot.model.Role;
import com.ra.base_spring_boot.model.UserMood;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ArtistResponseDTO {
    private Long artistId;
    private String firstName;
    private String lastName;
    private String profile_image;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String email;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String bio;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String status;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private LocalDateTime createdAt;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private LocalDateTime updatedAt;
}
