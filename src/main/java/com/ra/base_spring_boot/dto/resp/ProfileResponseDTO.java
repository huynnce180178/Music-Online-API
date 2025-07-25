package com.ra.base_spring_boot.dto.resp;

import com.ra.base_spring_boot.model.constants.UserStatus;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class ProfileResponseDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String profileImage;
    private String bio;
    private String createdAt;
    private String updatedAt;
    private Integer age;
    private String dateOfBirth;
}
