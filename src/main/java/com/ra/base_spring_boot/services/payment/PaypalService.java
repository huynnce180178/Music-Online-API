package com.ra.base_spring_boot.services.payment;

import com.paypal.api.payments.*;
import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.PayPalRESTException;
import com.ra.base_spring_boot.dto.req.NotificationRequest;
import com.ra.base_spring_boot.dto.resp.SubscriptionResponseDTO;
import com.ra.base_spring_boot.exception.HttpBadRequest;
import com.ra.base_spring_boot.model.Subscription;
import com.ra.base_spring_boot.model.constants.PaymentStatus;
import com.ra.base_spring_boot.repository.PaymentRepository;
import com.ra.base_spring_boot.services.notification.NotificationService;
import com.ra.base_spring_boot.services.subscription.SubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class PaypalService {
    @Autowired
    private APIContext apiContext;
    @Autowired
    PaymentRepository paymentRepository;
    @Autowired
    private SubscriptionService subscriptionService;
    @Autowired
    private NotificationService notificationService;

    public void updatePaymentStatus(Long paymentId, PaymentStatus status) {
        com.ra.base_spring_boot.model.Payment payment = paymentRepository.findById(paymentId).orElse(null);
        if (payment == null) {
            throw new HttpBadRequest("Payment not found");
        }
        payment.setPaymentStatus(status);
        paymentRepository.save(payment);
    }

    //tạo 1 payment
    //truyền vào số tiền, loại tiền tệ, thông tin đường dẫn cancel và successUrl
    public String createPayment(Double total, String currency, String description, String cancelUrl, String successUrl)
            throws PayPalRESTException {
        Amount amount = new Amount();
        amount.setTotal(total.toString());
        amount.setCurrency(currency);

        Transaction transaction = new Transaction();
        transaction.setAmount(amount);
        transaction.setDescription(description);

        Payer payer = new Payer();
        payer.setPaymentMethod("paypal");

        Payment payment = new Payment();
        payment.setIntent("sale");
        payment.setPayer(payer);
        payment.setTransactions(Arrays.asList(transaction));

        RedirectUrls redirectUrls = new RedirectUrls();
        redirectUrls.setCancelUrl(cancelUrl);
        redirectUrls.setReturnUrl(successUrl);
        payment.setRedirectUrls(redirectUrls);

        Payment createdPayment = payment.create(apiContext);

        List<Links> links = createdPayment.getLinks();
        for (Links link : links) {
            if ("approval_url".equalsIgnoreCase(link.getRel())) {
                return link.getHref();
            }
        }

        throw new PayPalRESTException("Approval URL not found");
    }

    public Payment executePayment(String paymentId, String payerId, Long userId, Long planId, Long paymentIdDB) throws PayPalRESTException {


        updatePaymentStatus(paymentIdDB, PaymentStatus.COMPLETED);
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


        //xữ lý của paypal
        Payment payment = new Payment();
        payment.setId(paymentId);
        PaymentExecution paymentExecution = new PaymentExecution();
        paymentExecution.setPayerId(payerId);
        return payment.execute(apiContext, paymentExecution);
    }

}
