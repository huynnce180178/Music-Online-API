package com.ra.base_spring_boot.dto.req;

import jakarta.mail.Multipart;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Email;
import org.springframework.web.multipart.MultipartFile;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class FormRegister
{
    @NotBlank(message = "First name cannot be blank!")
    private String firstName;
    @NotBlank(message = "Last name cannot be blank!")
    private String lastName;
    @NotBlank(message = "Mail cannot be blank!")
    @Email(message = "Invalid email format!")
    private String email;
    @NotBlank(message = "Password cannot be blank!")
    @Size(min = 6, max = 32, message = "Password must be between 6 and 32 characters.")
    @Pattern(regexp = ".*[A-Z].*", message = "Password must contain at least one uppercase letter.")
    private String password;
    private String bio;
    private MultipartFile profilePic;
}
