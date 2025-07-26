package com.ra.base_spring_boot.services.impl;

import com.ra.base_spring_boot.dto.resp.PaymentResponseDTO;
import com.ra.base_spring_boot.model.EmailVerification;
import com.ra.base_spring_boot.model.Subscription;
import com.ra.base_spring_boot.model.User;
import com.ra.base_spring_boot.repository.EmailVerificationRepository;
import com.ra.base_spring_boot.repository.IUserRepository;
import com.ra.base_spring_boot.services.EmailVerificationService;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

@Service
public class EmailVerificationServiceImpl implements EmailVerificationService {


    @Autowired
    private EmailVerificationRepository emailVerificationRepository;
    @Autowired
    private IUserRepository userRepository;
    @Autowired
    private JavaMailSender mailSender;
    @Autowired
    public EmailVerificationServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public String generateToken(User user) {
        String token = String.format("%06d", new java.util.Random().nextInt(1000000));
        EmailVerification emailVerification = new EmailVerification();
        emailVerification.setUser(user);
        emailVerification.setVerificationToken(token);
        emailVerification.setExpirationDate(java.time.LocalDateTime.now().plusMinutes(3));
        emailVerificationRepository.save(emailVerification);

        return token;
    }

    @Override
    public void sendVerificationEmail(User user, String token) {
        String subject = "Active Your Account";
        String activationUrl = "https://music-online-client-gcfmcqhfbzakh5ac.southeastasia-01.azurewebsites.net/auth/activate/" + token;
        String body = "<p>Click the link below to activate your account:</p><p><a href=\"" + activationUrl + "\">Activate Now</a></p>";

        sendEmail(user.getEmail(), subject, body);

    }
    @Override
    public void sendResetPasswordEmail(User user, String token) {
        String subject = "Reset your password with OTP code";
        String body = "<p>Your OTP code to reset your password is: <b>" + token + "</b></p>";
        sendEmail(user.getEmail(), subject, body);
    }

    @Override
    public void sendEmail(String to, String subject, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom("apimusiconline@gmail.com");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true);
            mailSender.send(message);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    @Override
    public EmailVerification findByVerificationToken(String verificationToken) {
        return emailVerificationRepository.findByVerificationToken(verificationToken);
    }
    @Override
    public void sendMailPaymentPaypal(User user, String approvalUrl, PaymentResponseDTO paymentInfo) {
        String subject = "🔔 Confirm Your Payment – Plan: " + paymentInfo.getPlanName();

        String body = """
        <div style="font-family: Arial, sans-serif; padding: 20px;">
            <h2>Hello %s %s,</h2>
            <p>Thank you for choosing our service! Below are your payment details:</p>

            <table style="border-collapse: collapse; margin: 15px 0;">
                <tr><td><strong>Plan:</strong></td><td>%s</td></tr>
                <tr><td><strong>Transaction ID:</strong></td><td>%s</td></tr>
                <tr><td><strong>Amount:</strong></td><td>%.2f USD</td></tr>
                <tr><td><strong>Payment Method:</strong></td><td>%s</td></tr>
                <tr><td><strong>Status:</strong></td><td>%s</td></tr>
                <tr><td><strong>Date:</strong></td><td>%s</td></tr>
            </table>

            <p style="margin: 25px 0;">
                <a href="%s" style="background-color: #0070ba; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; display: inline-block;">
                    🧾 Complete Payment with PayPal
                </a>
            </p>

            <p>If you did not request this payment, please ignore this email.</p>
            <hr>
            <p style="font-size: 12px; color: gray;">This is an automated email. Please do not reply.</p>
        </div>
        """.formatted(
                paymentInfo.getFirstName(),
                paymentInfo.getLastName(),
                paymentInfo.getPlanName(),
                paymentInfo.getTransitionId(),
                paymentInfo.getAmount(),
                paymentInfo.getPaymentMethod(),
                paymentInfo.getPaymentStatus(),
                paymentInfo.getPaymentDate().toString(),
                approvalUrl
        );

        sendEmail(user.getEmail(), subject, body);
    }
    @Override
    public void sendMailExpire(Subscription subscription) {
        User user = subscription.getUser();
        String subject = "Notice: Your Subscription Has Expired";

        String body = String.format(
                "Dear %s %s,\n\n" +
                        "We would like to inform you that your **%s** subscription has expired on %s.\n\n" +
                        "To continue enjoying our premium features and services, please renew or upgrade your subscription from your account settings.\n\n" +
                        "If you have any questions or need assistance, feel free to contact our support team.\n\n" +
                        "Best regards,\nSupport Team",
                user.getFirstName(),
                user.getLastName(),
                subscription.getPlan().name(),
                subscription.getEndTime().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
        );

        sendEmail(user.getEmail(), subject, body);
    }
    @Override
    public void setMail(String facebookId, String newEmail) {
        User user = userRepository.findByFacebookId(facebookId).orElseThrow(() -> new RuntimeException("User not found"));
        String otp = generateToken(user);
        String subject = "Verify your new email for Facebook account";
        String body = "<p>Your OTP code to verify your email is: <b>" + otp + "</b></p>";
        sendEmail(newEmail, subject, body);
        EmailVerification ev = emailVerificationRepository.findByVerificationToken(otp);
        if (ev != null) {
            ev.setTempEmail(newEmail);
            emailVerificationRepository.save(ev);
        }
    }
    @Override
    public boolean verifyMail(String facebookId, String otp) {
        User user = userRepository.findByFacebookId(facebookId).orElseThrow(() -> new RuntimeException("User not found"));
        EmailVerification ev = emailVerificationRepository.findByVerificationToken(otp);
        if (ev != null && ev.getUser().getId().equals(user.getId()) && ev.getExpirationDate().isAfter(java.time.LocalDateTime.now())) {
            String newEmail = ev.getTempEmail();
            if (newEmail != null && !newEmail.isEmpty()) {
                user.setEmail(newEmail);
                userRepository.save(user);
                emailVerificationRepository.delete(ev);
                return true;
            }
        }
        return false;
    }

}
