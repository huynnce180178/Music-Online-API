package com.ra.base_spring_boot.controller.Payment;

import com.ra.base_spring_boot.dto.ResponseWrapper;
import com.ra.base_spring_boot.dto.req.PaymentRequestDTO;
import com.ra.base_spring_boot.dto.resp.AdvertisementResponse;
import com.ra.base_spring_boot.dto.resp.PaymentResponseDTO;
import com.ra.base_spring_boot.model.constants.PaymentMethod;
import com.ra.base_spring_boot.security.principle.MyUserDetails;
import com.ra.base_spring_boot.services.payment.PaymentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name="Payment online controller")
@RestController
@RequestMapping("/api/v1/user/payment")
@Validated
public class PaymentController {
    @Autowired
    private PaymentService paymentService;

    @PostMapping()
    public ResponseEntity<?> createPayment(
            @AuthenticationPrincipal MyUserDetails userDetails,
            @RequestBody PaymentRequestDTO paymentRequestDTO,
            HttpServletRequest request) {
        String urlPayment;
        if(paymentRequestDTO.getPaymentMethod().equals(PaymentMethod.PAYPAL)) {
            urlPayment = paymentService.createPaymentWithPaypal(userDetails.getUser().getId(), paymentRequestDTO);
        }else if(paymentRequestDTO.getPaymentMethod().equals(PaymentMethod.VNPAY)) {
            urlPayment = paymentService.createPaymentWithVnpay(userDetails.getUser().getId(), paymentRequestDTO);
        }else if(paymentRequestDTO.getPaymentMethod().equals(PaymentMethod.ZALO_PAY)) {
            urlPayment = paymentService.createPaymentWithZalopay(userDetails.getUser().getId(), paymentRequestDTO);
        }else {
            // Trường hợp không hợp lệ
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    ResponseWrapper.builder()
                            .data(null)
                            .code(HttpStatus.BAD_REQUEST.value())
                            .status(HttpStatus.BAD_REQUEST)
                            .message("Invalid payment method!")
                            .build()
            );
        }
        return ResponseEntity.ok(ResponseWrapper.builder()
                .data(urlPayment)
                .code(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build());
    }
}
