package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.model.EmailVerification;
import com.ra.base_spring_boot.model.User;
import com.ra.base_spring_boot.model.constants.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {
    EmailVerification findByVerificationToken(String verificationToken);
    EmailVerification findByUser_Email(String email);
}
