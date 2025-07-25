package com.ra.base_spring_boot.Configuration;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

@Data
@Component
@ConfigurationProperties(prefix = "vosk.model")
public class VoskModelConfig {
    private String vn;
    private String en;}
