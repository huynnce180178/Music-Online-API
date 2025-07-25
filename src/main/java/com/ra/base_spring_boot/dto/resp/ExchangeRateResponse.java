package com.ra.base_spring_boot.dto.resp;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class ExchangeRateResponse {
    private String result;
    private Map<String, Double> rates;
}
