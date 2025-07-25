package com.ra.base_spring_boot.Configuration;

import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class ZalopayConfig {

    public static final Map<String, String> config = new HashMap<String, String>() {
        {
            put("app_id", "554");
            put("key1", "8NdU5pG5R2spGHGhyO99HN1OhD8IQJBn");
            put("key2", "uUfsWgfLkRLzq6W2uNXTCxrfxs51auny");
            put("endpoint", "https://sb-openapi.zalopay.vn/v2/create");
            put("orderstatus", "https://sb-openapi.zalopay.vn/v2/query");

        }
    };
}