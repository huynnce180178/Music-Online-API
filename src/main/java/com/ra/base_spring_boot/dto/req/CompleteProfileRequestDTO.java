package com.ra.base_spring_boot.dto.req;

import lombok.Data;

@Data
public class CompleteProfileRequestDTO {
    private String firstName;
    private String lastName;
    private String profileImage;
    private String bio;
    private String facebookId;
    private String type;
}
