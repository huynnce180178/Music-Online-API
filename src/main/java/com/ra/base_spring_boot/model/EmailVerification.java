package com.ra.base_spring_boot.model;

import com.ra.base_spring_boot.model.base.BaseObject;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "email_verification")
public class EmailVerification extends BaseObject {
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String verificationToken;
    private LocalDateTime expirationDate;
    private String tempEmail;
}
