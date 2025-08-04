package com.ra.base_spring_boot.services.payment;

import com.ra.base_spring_boot.dto.req.PaymentRequestDTO;
import com.ra.base_spring_boot.dto.req.PaypalRequestDTO;
import com.ra.base_spring_boot.dto.resp.PaymentResponseDTO;
import com.ra.base_spring_boot.model.constants.PaymentStatus;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface PaymentService {
    List<PaymentResponseDTO> getAll();
    PaymentResponseDTO getById(Long id);
    String createPaymentWithPaypal(Long id,PaymentRequestDTO paymentRequestDTO);
    void updatePaymentStatus(Long paymentId, PaymentStatus status);
    void delete(Long id);

    String createPaymentWithVnpay(Long id,PaymentRequestDTO paymentRequestDTO, HttpServletRequest request);
    String createPaymentWithZalopay(Long id,PaymentRequestDTO paymentRequestDTO);

}
