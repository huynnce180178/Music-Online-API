package com.ra.base_spring_boot.controller.Payment;

import com.ra.base_spring_boot.dto.req.VnpayRequestDTO;
import com.ra.base_spring_boot.model.constants.PaymentStatus;
import com.ra.base_spring_boot.security.principle.MyUserDetails;
import com.ra.base_spring_boot.services.payment.PaymentService;
import com.ra.base_spring_boot.services.payment.VnpayService;
import com.ra.base_spring_boot.services.subscription.SubscriptionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jdk.jshell.JShell;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@Tag(name="Payment vnpay")
@RequestMapping("/api/v1/user/payment/vnpay")
@AllArgsConstructor
public class VnpayController {

    private final VnpayService vnpayService;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private SubscriptionService subscriptionService;

//    @PostMapping
//    public ResponseEntity<String> createPayment(@RequestBody VnpayRequestDTO paymentRequest) {
//        try {
//            String paymentUrl = vnpayService.createPayment(paymentRequest);
//            return ResponseEntity.ok(paymentUrl);
//        } catch (IllegalArgumentException e) {
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Đã xảy ra lỗi khi tạo thanh toán!");
//        }
//    }

        @GetMapping("/return")
    public ResponseEntity<String> returnPayment(@RequestParam("vnp_ResponseCode") String responseCode,
                                                @RequestParam("userId") Long userId,
                                                @RequestParam("subscriptionPlanId") Long planId,
                                                @RequestParam("paymentId") Long paymentId,
                                                HttpServletResponse response) {

        return vnpayService.handlePaymentReturn(userId ,responseCode, planId, paymentId, response);
    }
}