package com.ra.base_spring_boot.controller;

import com.ra.base_spring_boot.dto.ResponseWrapper;
import com.ra.base_spring_boot.dto.req.SubscriptionPlanRequestDTO;

import com.ra.base_spring_boot.services.subscription_plan.SubscriptionPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name="Subscription Plan Controller")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SubscriptionPlanController {
    @Autowired
    private SubscriptionPlanService subscriptionPlanService;

    @Operation(summary = "Get all subscription plans", description = "Retrieve all available subscription plans in the system")
    @GetMapping("/subscriptionPlan")
    public ResponseEntity<?> getSubscriptionPlan() {
        return ResponseEntity.ok().body(
                ResponseWrapper.builder()
                        .status(HttpStatus.OK)
                        .code(200)
                        .data(subscriptionPlanService.getAllSubscriptionPlan())
                        .build());
    }
    @Operation(summary = "Get subscription plan by ID", description = "Retrieve a specific subscription plan by its ID")
    @GetMapping("/subscriptionPlan/{id}")
    public ResponseEntity<?> getSubscriptionPlanById(@PathVariable Long id) {
        return ResponseEntity.ok().body(
                ResponseWrapper.builder()
                        .status(HttpStatus.OK)
                        .code(200)
                        .data(subscriptionPlanService.getSubscriptionPlanById(id))
                        .build());
    }
    @Operation(summary = "Add a new subscription plan", description = "Create a new subscription plan")
    @PostMapping("/admin/subscriptionPlan/add")
    public ResponseEntity<?> addSubscriptionPlan(@RequestBody SubscriptionPlanRequestDTO subscriptionPlanRequestDTO) {
        return ResponseEntity.ok().body(
                ResponseWrapper.builder()
                        .status(HttpStatus.OK)
                        .code(200)
                        .data(subscriptionPlanService.saveSubscriptionPlan(subscriptionPlanRequestDTO))
                        .build());
    }
    @Operation(summary = "Update a subscription plan", description = "Update an existing subscription plan by its ID")
    @PostMapping("/admin/subscriptionPlan/update/{id}")
    public ResponseEntity<?> updateSubscriptionPlan(@RequestBody SubscriptionPlanRequestDTO subscriptionPlanRequestDTO, @PathVariable Long id) {
        return ResponseEntity.ok().body(
                ResponseWrapper.builder()
                        .status(HttpStatus.OK)
                        .code(200)
                        .data(subscriptionPlanService.updateSubscriptionPlan(id, subscriptionPlanRequestDTO))
                        .build());
    }
    @Operation(summary = "Delete a subscription plan", description = "Delete a subscription plan by its ID")
    @DeleteMapping("/admin/subscriptionPlan/delete/{id}")
    public ResponseEntity<?> deleteSubscriptionPlan(@PathVariable Long id) {
        subscriptionPlanService.deleteSubscriptionPlan(id);
        return ResponseEntity.ok().build();
    }

}
