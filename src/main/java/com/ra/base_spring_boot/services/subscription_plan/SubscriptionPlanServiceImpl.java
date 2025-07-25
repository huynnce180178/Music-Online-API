package com.ra.base_spring_boot.services.subscription_plan;

import com.ra.base_spring_boot.dto.req.SubscriptionPlanRequestDTO;
import com.ra.base_spring_boot.dto.resp.SubscriptionPlanResponseDTO;
import com.ra.base_spring_boot.exception.HttpBadRequest;
import com.ra.base_spring_boot.model.SubscriptionPlan;
import com.ra.base_spring_boot.repository.SubscriptionPlanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SubscriptionPlanServiceImpl implements SubscriptionPlanService {


    @Autowired
    private SubscriptionPlanRepository subscriptionPlanRepository;

    private SubscriptionPlanResponseDTO mapToDTO(SubscriptionPlan subscriptionPlan) {
        return SubscriptionPlanResponseDTO.builder()
                .id(subscriptionPlan.getId())
                .planName(subscriptionPlan.getPlanName())
                .price(subscriptionPlan.getPrice())
                .planType(subscriptionPlan.getPlanType())
                .durationDay(subscriptionPlan.getDurationDay())
                .description(subscriptionPlan.getDescription())
                .build();
    }
    private SubscriptionPlan mapToEntity(SubscriptionPlanRequestDTO subscriptionPlanRequestDTO) {
        return SubscriptionPlan.builder()
                .planName(subscriptionPlanRequestDTO.getPlanName())
                .price(subscriptionPlanRequestDTO.getPrice())
                .planType(subscriptionPlanRequestDTO.getPlanType())
                .durationDay(subscriptionPlanRequestDTO.getDurationDay())
                .description(subscriptionPlanRequestDTO.getDescription())
                .build();
    }

    @Override
    public List<SubscriptionPlanResponseDTO> getAllSubscriptionPlan() {
        List<SubscriptionPlan> subscriptionPlans = subscriptionPlanRepository.findAll();
        List<SubscriptionPlanResponseDTO> subscriptionPlanResponseDTO = subscriptionPlans.stream().map(subscriptionPlan -> mapToDTO(subscriptionPlan)).collect(Collectors.toList());
        return subscriptionPlanResponseDTO;
    }

    @Override
    public SubscriptionPlanResponseDTO getSubscriptionPlanById(Long id) {
        SubscriptionPlan subscriptionPlan = subscriptionPlanRepository.findById(id).orElse(null);
        if (subscriptionPlan == null) {
            throw new HttpBadRequest("Subscription Plan not found");
        }
        return mapToDTO(subscriptionPlan);
    }
    @Override
    public SubscriptionPlanResponseDTO saveSubscriptionPlan(SubscriptionPlanRequestDTO subscriptionPlanRequestDTO) {
        SubscriptionPlan subscriptionPlan = mapToEntity(subscriptionPlanRequestDTO);
        subscriptionPlan.setCreatedAt(LocalDateTime.now());
        subscriptionPlan.setUpdatedAt(LocalDateTime.now());
        subscriptionPlanRepository.save(subscriptionPlan);
        return mapToDTO(subscriptionPlan);
    }

    @Override
    public SubscriptionPlanResponseDTO updateSubscriptionPlan(Long id ,SubscriptionPlanRequestDTO subscriptionPlanRequestDTO) {
        SubscriptionPlan subscriptionPlan = subscriptionPlanRepository.findById(id).orElse(null);
        if (subscriptionPlan == null) {
            throw new HttpBadRequest("Subscription Plan not found");
        }
        subscriptionPlan.setPlanName(subscriptionPlanRequestDTO.getPlanName());
        subscriptionPlan.setPrice(subscriptionPlanRequestDTO.getPrice());
        subscriptionPlan.setDurationDay(subscriptionPlanRequestDTO.getDurationDay());
        subscriptionPlan.setDescription(subscriptionPlanRequestDTO.getDescription());
        subscriptionPlan.setUpdatedAt(LocalDateTime.now());
        subscriptionPlanRepository.save(subscriptionPlan);
        return mapToDTO(subscriptionPlan);
    }

    @Override
    public void deleteSubscriptionPlan(Long id) {
        subscriptionPlanRepository.deleteById(id);
    }

}
