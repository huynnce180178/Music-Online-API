package com.ra.base_spring_boot.Configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DomainConfig {
    
    @Value("${domain.frontend}")
    private String frontendDomain;
    
    @Value("${domain.backend}")
    private String backendDomain;
    
    public String getFrontendDomain() {
        return frontendDomain;
    }
    
    public String getBackendDomain() {
        return backendDomain;
    }
} 