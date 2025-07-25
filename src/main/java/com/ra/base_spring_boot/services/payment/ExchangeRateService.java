package com.ra.base_spring_boot.services.payment;

import com.ra.base_spring_boot.dto.resp.ExchangeRateResponse;
import lombok.Setter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class ExchangeRateService {
    public Double getUSDtoVND() {String url = "https://open.er-api.com/v6/latest/USD";
        RestTemplate restTemplate = new RestTemplate();
        ExchangeRateResponse response = restTemplate.getForObject(url, ExchangeRateResponse.class);

        if (response != null && response.getRates() != null) {
            return response.getRates().get("VND");
        }
        return null;
    }
}
