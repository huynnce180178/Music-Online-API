package com.ra.base_spring_boot.services.payment;

import com.ra.base_spring_boot.Configuration.VnpayConfig;
import com.ra.base_spring_boot.dto.req.NotificationRequest;
import com.ra.base_spring_boot.dto.req.VnpayRequestDTO;
import com.ra.base_spring_boot.dto.resp.SubscriptionResponseDTO;
import com.ra.base_spring_boot.exception.HttpBadRequest;
import com.ra.base_spring_boot.model.Payment;
import com.ra.base_spring_boot.model.Subscription;
import com.ra.base_spring_boot.model.constants.PaymentStatus;
import com.ra.base_spring_boot.repository.PaymentRepository;
import com.ra.base_spring_boot.services.notification.NotificationService;
import com.ra.base_spring_boot.services.subscription.SubscriptionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class VnpayService {

    @Autowired
    PaymentRepository paymentRepository;
    @Autowired
    private SubscriptionService subscriptionService;
    @Autowired
    private NotificationService notificationService;

    public void updatePaymentStatus(Long paymentId, PaymentStatus status) {
        Payment payment = paymentRepository.findById(paymentId).orElse(null);
        if (payment == null) {
            throw new HttpBadRequest("Payment not found");
        }
        payment.setPaymentStatus(status);
        paymentRepository.save(payment);
    }

    public String createPayment(VnpayRequestDTO paymentRequest, Long userId, Long planId, Long paymentId, HttpServletRequest request) throws UnsupportedEncodingException {
        String vnp_Version = "2.1.0";
        String vnp_Command = "pay";
        String orderType = "other";

        long amount = 0;
        try {
            amount = Math.round(paymentRequest.getAmount() * 100);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid amount");
        }

        String bankCode = "NCB";
        String vnp_TxnRef = VnpayConfig.getRandomNumber(8);
        // String vnp_IpAddr = "127.0.0.1";
        String vnp_IpAddr = VnpayConfig.getIpAddress(request); // lấy IP thật
        String vnp_TmnCode = VnpayConfig.vnp_TmnCode;

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amount));
        vnp_Params.put("vnp_CurrCode", "VND");

        vnp_Params.put("vnp_BankCode", bankCode);
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", "Thanh toan don hang:" + vnp_TxnRef);
        vnp_Params.put("vnp_OrderType", orderType);
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", VnpayConfig.vnp_ReturnUrl + "?userId=" + userId + "&subscriptionPlanId=" + planId + "&paymentId=" + paymentId);
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);
//        // Thêm các tham số userId, subscriptionPlanId, paymentId vào vnp_Params
//        vnp_Params.put("userId", String.valueOf(userId));
//        vnp_Params.put("subscriptionPlanId", String.valueOf(planId));
//        vnp_Params.put("paymentId", String.valueOf(paymentId));

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        cld.add(Calendar.MINUTE, 120);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        for (String fieldName : fieldNames) {
            String fieldValue = vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                hashData.append(fieldName).append('=')
                        .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()))
                        .append('=')
                        .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                query.append('&');
                hashData.append('&');
            }
        }

        if (query.length() > 0)
            query.setLength(query.length() - 1);
        if (hashData.length() > 0)
            hashData.setLength(hashData.length() - 1);

        String vnp_SecureHash = VnpayConfig.hmacSHA512(VnpayConfig.secretKey, hashData.toString());
        query.append("&vnp_SecureHash=").append(vnp_SecureHash);
        return VnpayConfig.vnp_PayUrl + "?" + query;
    }

    public ResponseEntity<String> handlePaymentReturn(Long userId, String responseCode, Long planId, Long paymentId, HttpServletResponse response) {
        try {
            System.out.println("[INFO] Payment return received - userId: " + userId + ", responseCode: " + responseCode + ", planId: " + planId + ", paymentId: " + paymentId);

            if ("00".equals(responseCode)) {
                updatePaymentStatus(paymentId, PaymentStatus.COMPLETED);
                SubscriptionResponseDTO subscription = subscriptionService.subscribe(userId,planId);
                String ownerMessage = String.format(
                        "You have successfully subscribed to the '%s' plan. Your subscription will expire in %d days.",
                        subscription.getPlan(),
                        subscription.getDaysRemaining()
                );

                notificationService.createNotification(
                        new NotificationRequest(
                                userId,
                                ownerMessage,
                                "Subscription Confirmed"
                        )
                );
                String redirectUrl = "https://music-online-client-gcfmcqhfbzakh5ac.southeastasia-01.azurewebsites.net/payment/success?paymentId=" + paymentId;
                response.sendRedirect(redirectUrl);
                return ResponseEntity.ok("Payment successful!");
            } else {
                updatePaymentStatus(paymentId, PaymentStatus.FAILED);
                String failUrl = "https://music-online-client-gcfmcqhfbzakh5ac.southeastasia-01.azurewebsites.net/payment/fail?message=" + URLEncoder.encode("Error code: " + responseCode, StandardCharsets.UTF_8);
                response.sendRedirect(failUrl);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Payment failed! Error code: " + responseCode);
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Exception during payment return handling: " + e.getMessage());

            try {
                String errorUrl = "https://music-online-client-gcfmcqhfbzakh5ac.southeastasia-01.azurewebsites.net/payment/fail?message=" + URLEncoder.encode("System error: " + e.getMessage(), StandardCharsets.UTF_8);
                response.sendRedirect(errorUrl);
            } catch (Exception ex) {
                System.err.println("[ERROR] Failed to redirect after exception: " + ex.getMessage());
            }

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("System error: " + e.getMessage());
        }
    }

}

