package com.ra.base_spring_boot.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestLogController {
    private static final Logger logger = LoggerFactory.getLogger(TestLogController.class);

    @GetMapping("/test-log")
    public String testLog() {
        logger.info(">>> This is a test INFO log");
        logger.warn(">>> This is a test WARNING log");
        logger.error(">>> This is a test ERROR log");
        return "Logs have been written!";
    }
}