package com.ra.base_spring_boot.services.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paypal.base.rest.PayPalRESTException;
import com.ra.base_spring_boot.dto.req.PaymentRequestDTO;
import com.ra.base_spring_boot.dto.req.VnpayRequestDTO;
import com.ra.base_spring_boot.dto.resp.PaymentResponseDTO;
import com.ra.base_spring_boot.exception.HttpBadRequest;
import com.ra.base_spring_boot.model.Payment;
import com.ra.base_spring_boot.model.constants.PaymentMethod;
import com.ra.base_spring_boot.model.constants.PaymentStatus;
import com.ra.base_spring_boot.repository.IUserRepository;
import com.ra.base_spring_boot.repository.PaymentRepository;
import com.ra.base_spring_boot.repository.SubscriptionPlanRepository;
import com.ra.base_spring_boot.services.EmailVerificationService;
import jakarta.servlet.http.HttpServletRequest;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;


import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private VnpayService vnpayService;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private IUserRepository userRepository;
    @Autowired
    private SubscriptionPlanRepository subscriptionPlanRepository;
    @Autowired
    private PaypalService paypalService;
    @Autowired
    private EmailVerificationService emailVerificationService;
    @Autowired
    private ExchangeRateService exchangeRateService;
    @Autowired
    private ZalopayService zalopayService;

    private PaymentResponseDTO mapToDTO(Payment payment) {
        return PaymentResponseDTO.builder()
                .firstName(payment.getUsers().getFirstName())
                .lastName(payment.getUsers().getLastName())
                .planName(payment.getSubscription_plan().getPlanName())
                .paymentMethod(payment.getPaymentMethod())
                .paymentDate(payment.getPaymentDate())
                .paymentStatus(payment.getPaymentStatus())
                .transitionId(payment.getTransactionId())
                .build();
    };
    private Payment mapToEntity(PaymentRequestDTO paymentRequestDTO) {
        return Payment.builder()
                .subscription_plan(subscriptionPlanRepository.getById(paymentRequestDTO.getPlanId()))
                .amount(subscriptionPlanRepository.getById(paymentRequestDTO.getPlanId()).getPrice())
                .transactionId(paymentRequestDTO.getTransitionId())
                .paymentMethod(paymentRequestDTO.getPaymentMethod())
                .build();
    }

    @Override
    public List<PaymentResponseDTO> getAll() {
        List<Payment> payments = paymentRepository.findAll();
        List<PaymentResponseDTO> dtos = payments.stream().map(p -> mapToDTO(p)).collect(Collectors.toList());
        return dtos;
    }


    @Override
    public String createPaymentWithPaypal(Long userId, PaymentRequestDTO paymentRequestDTO) {
        Payment payment = mapToEntity(paymentRequestDTO);
        payment.setUsers(userRepository.getById(userId));
        payment.setPaymentDate(LocalDateTime.now());
        payment.setCreatedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());
        payment.setPaymentStatus(PaymentStatus.PENDING);
        paymentRepository.save(payment);
        String approvalUrl = null;
        if (payment.getPaymentMethod()!=null && payment.getPaymentMethod() == PaymentMethod.PAYPAL) {
            try {
                approvalUrl = paypalService.createPayment(payment.getAmount(), "USD",
                        "Payment for Order",
                        "https://music-online-backend-fjgha0e2bvevfah8.southeastasia-01.azurewebsites.net/api/v1/user/payment/paypal/cancel?paymentId="+payment.getId(),
                        "https://music-online-backend-fjgha0e2bvevfah8.southeastasia-01.azurewebsites.net/api/v1/user/payment/paypal/success?userId="+userId + "&subscriptionPlanId="+paymentRequestDTO.getPlanId() +"&paymentIdDB=" + payment.getId());
                emailVerificationService.sendMailPaymentPaypal(userRepository.getById(userId), approvalUrl, mapToDTO(payment));
                return approvalUrl;

            } catch (PayPalRESTException e) {
                throw new RuntimeException("Error creating payment: " + e.getMessage());
            }
        }
        return approvalUrl;
    }
    @Override
    public void delete(Long id) {
        Payment payment = paymentRepository.findById(id).orElse(null);
    }
    @Override
    public void updatePaymentStatus(Long paymentId, PaymentStatus status) {
        Payment payment = paymentRepository.findById(paymentId).orElse(null);
        if (payment == null) {
            throw new HttpBadRequest("Payment not found");
        }
        payment.setPaymentStatus(status);
        paymentRepository.save(payment);
    }


    @Override
    public String createPaymentWithVnpay(Long userId, PaymentRequestDTO paymentRequestDTO, HttpServletRequest request) {
        Payment payment = mapToEntity(paymentRequestDTO);
        payment.setUsers(userRepository.getById(userId));
        payment.setPaymentDate(LocalDateTime.now());
        payment.setCreatedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());
        payment.setPaymentStatus(PaymentStatus.PENDING);
        paymentRepository.save(payment);

        VnpayRequestDTO vnpayRequest = new VnpayRequestDTO();
        Double totalAmount = payment.getAmount() * exchangeRateService.getUSDtoVND();
        vnpayRequest.setAmount(totalAmount);
        String paymentUrl;
        try {
            paymentUrl = vnpayService.createPayment(vnpayRequest, userId, paymentRequestDTO.getPlanId(), payment.getId(),request );
            emailVerificationService.sendMailPaymentPaypal(userRepository.getById(userId), paymentUrl, mapToDTO(payment));
        } catch (Exception ex) {
            throw new RuntimeException("Unable to create VNPAY payment link", ex);
        }

        return paymentUrl;
    }
    @Override
    public String createPaymentWithZalopay(Long id, PaymentRequestDTO paymentRequestDTO) {
        Payment payment = mapToEntity(paymentRequestDTO);
        payment.setUsers(userRepository.getById(id));
        payment.setPaymentDate(LocalDateTime.now());
        payment.setCreatedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());
        payment.setPaymentStatus(PaymentStatus.PENDING);
        paymentRepository.save(payment);
        Double totalAmount = payment.getAmount() * exchangeRateService.getUSDtoVND();
        String paymentUrl;
        try {
            String jsonString  = zalopayService.createOrder(id,totalAmount);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(jsonString);

            paymentUrl = root.get("order_url").asText();
            emailVerificationService.sendMailPaymentPaypal(userRepository.getById(id), paymentUrl, mapToDTO(payment));
        } catch (Exception ex) {
            throw new RuntimeException("Unable to create VNPAY payment link", ex);
        }
        return paymentUrl;
    }

}
