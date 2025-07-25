package com.ra.base_spring_boot.controller;

import com.ra.base_spring_boot.dto.ResponseWrapper;
import com.ra.base_spring_boot.model.User;
import com.ra.base_spring_boot.repository.SubscriptionRepository;
import com.ra.base_spring_boot.security.principle.MyUserDetails;
import com.ra.base_spring_boot.services.subscription.SubscriptionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.web.servlet.function.ServerResponse.ok;

@Tag(name="Subscription Controller")
@RestController
@RequestMapping("/api/v1/user/subscription")
@RequiredArgsConstructor
public class SubscriptionController {
    @Autowired
    private final SubscriptionService subscriptionService;


    @GetMapping()
    public ResponseEntity<?> getMySubscription(@AuthenticationPrincipal MyUserDetails userDetails) {
        User user = userDetails.getUser();
        return ResponseEntity.ok().body(
                ResponseWrapper.builder()
                        .status(HttpStatus.OK)
                        .code(200)
                        .data(subscriptionService.getSubscriptionByUser(user))
                        .build());
    }
}
