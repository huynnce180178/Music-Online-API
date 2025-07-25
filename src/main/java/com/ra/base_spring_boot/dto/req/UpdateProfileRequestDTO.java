package com.ra.base_spring_boot.dto.req;

import lombok.*;
import org.springframework.web.multipart.MultipartFile;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class UpdateProfileRequestDTO {
    private String firstName;
    private String lastName;
    private String bio;
    private MultipartFile file;
    private String dateOfBirth;
}
