package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
}
