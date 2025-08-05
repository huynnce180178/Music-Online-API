package com.ra.base_spring_boot.controller.Payment;

import com.paypal.api.payments.Payment;
import com.paypal.base.rest.PayPalRESTException;
import com.ra.base_spring_boot.Configuration.DomainConfig;
import com.ra.base_spring_boot.dto.req.PaypalRequestDTO;
import com.ra.base_spring_boot.dto.resp.SubscriptionResponseDTO;
import com.ra.base_spring_boot.model.Subscription;
import com.ra.base_spring_boot.model.constants.PaymentStatus;
import com.ra.base_spring_boot.security.principle.MyUserDetails;
import com.ra.base_spring_boot.services.payment.PaymentService;
import com.ra.base_spring_boot.services.payment.PaypalService;
import com.ra.base_spring_boot.services.subscription.SubscriptionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;

import java.io.IOException;

@Tag(name = "Payment paypal")
@RestController
@RequestMapping("/api/v1/user/payment/paypal")
@Validated
public class PaypalController {

    @Autowired
    private PaypalService paypalService;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private DomainConfig domainConfig;



//    @PostMapping()
//    public ResponseEntity<String> createPayment(@RequestBody PaypalRequestDTO paypalRequestDTO) {
//        double amount;
//
//        try {
//            amount = Double.parseDouble(paypalRequestDTO.getTotal()) * 100.0;
//        } catch (NumberFormatException e) {
//            return ResponseEntity.badRequest().body("Số tiền không hợp lệ");
//        }
//
//        try {
//            String approvalUrl = paypalService.createPayment(
//                    amount,
//                    paypalRequestDTO.getCurrency(),
//                    "Payment for Order",
//                    "http://localhost:8080/api/v1/user/payment/paypal/cancel",
//                    "http://localhost:8080/api/v1/user/payment/paypal/success"
//            );
//            return ResponseEntity.ok(approvalUrl);
//        } catch (PayPalRESTException e) {
//            e.printStackTrace();
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body("Error creating payment: " + e.getMessage());
//        }
//    }

    @GetMapping("/success")
    public void success(@RequestParam("userId") Long userId,
                        @RequestParam("subscriptionPlanId") Long planId,
                        @RequestParam("paymentId") String paypalId,
                        @RequestParam("paymentIdDB") Long paymentIdDB,
                        @RequestParam("PayerID") String payerId,
                        HttpServletResponse response) throws IOException {
        try {
            Payment payment = paypalService.executePayment(paypalId, payerId, userId, planId, paymentIdDB);

            // Redirect to FE success page
            String redirectUrl = domainConfig.getFrontend() + "/payment/success?paymentId=" + payment.getId();
            response.sendRedirect(redirectUrl);

        } catch (PayPalRESTException e) {
            e.printStackTrace();

            // Redirect to FE fail page
            String failUrl = domainConfig.getFrontend() + "/payment/fail?message=" + e.getMessage();
            response.sendRedirect(failUrl);
        }
    }

    @GetMapping("/cancel")
    public void cancel(@RequestParam("paymentId") Long paymentId,
                       HttpServletResponse response) throws IOException {
        paymentService.updatePaymentStatus(paymentId, PaymentStatus.FAILED);

        // Redirect to FE fail page
        String failUrl = domainConfig.getFrontend() + "/payment/fail?message=Payment+was+canceled";
        response.sendRedirect(failUrl);
    }
}
