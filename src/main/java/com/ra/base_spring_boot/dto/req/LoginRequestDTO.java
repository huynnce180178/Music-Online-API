package com.ra.base_spring_boot.dto.req;

import lombok.Data;

@Data
public class LoginRequestDTO {
    private String email;
    private String password;
    private String facebookId;
    private String name;
    private String type;
}
