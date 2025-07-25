package com.ra.base_spring_boot.services;

import com.ra.base_spring_boot.dto.resp.PaymentResponseDTO;
import com.ra.base_spring_boot.model.EmailVerification;
import com.ra.base_spring_boot.model.Subscription;
import com.ra.base_spring_boot.model.User;

public interface EmailVerificationService {
    EmailVerification findByVerificationToken(String verificationToken);
    String generateToken(User user);
    void sendVerificationEmail(User user, String token);
    void sendEmail(String to, String subject, String body);
    void sendResetPasswordEmail(User user, String token);
    void sendMailPaymentPaypal(User user, String approvalUrl, PaymentResponseDTO paymentInfo);
    void sendMailExpire(Subscription subscription);
    void setMail(String facebookId, String newEmail);
    boolean verifyMail(String facebookId, String otp);
}
