package com.ra.base_spring_boot.controller.Payment;

import com.ra.base_spring_boot.security.principle.MyUserDetails;
import com.ra.base_spring_boot.services.payment.ZalopayService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
@Tag(name="Payment zalopay")
@RestController
@RequestMapping("/api/v1/user/payment/zalopay")
@Validated
public class ZalopayController {

    @Autowired
    private ZalopayService zalopayService;

    @PostMapping
    public ResponseEntity<String> createPayment(@RequestParam Double amount,
                                                @AuthenticationPrincipal MyUserDetails userDetails) {
        try {
            Long userId = userDetails.getUser().getId();
            String response = zalopayService.createOrder(userId,amount);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error creating payment: " + e.getMessage());
        }
    }

    @PostMapping("/order-status/{appTransId}")
    public ResponseEntity<String> getOrderStatus(@PathVariable String appTransId,
                                                 @AuthenticationPrincipal MyUserDetails userDetails) {
        Long userId = userDetails.getUser().getId();
        String response = zalopayService.getOrderStatus(userId ,appTransId);
        return ResponseEntity.ok(response);
    }

}
