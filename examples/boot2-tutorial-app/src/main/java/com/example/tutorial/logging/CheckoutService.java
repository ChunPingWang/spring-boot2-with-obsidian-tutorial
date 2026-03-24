package com.example.tutorial.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CheckoutService {

    private static final Logger logger = LoggerFactory.getLogger(CheckoutService.class);

    public String checkout(String orderNo) {
        logger.info("processing order {}", orderNo);
        return "checked-out-" + orderNo;
    }
}
