package com.ra.base_spring_boot.dto.resp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ra.base_spring_boot.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class JwtResponse
{
    private String accessToken;
    private final String type = "Bearer";
    @JsonIgnoreProperties({"roles","password"})
    private Long userId;
    private String userName;
    private String fullName;
    private String profileImage;
    private Set<String> roles;
}
