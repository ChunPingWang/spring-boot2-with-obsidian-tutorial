package com.example.tutorial.aop;

import org.springframework.stereotype.Service;

@Service
public class MonitoredBusinessService {

    public String process(String value) {
        return "processed-" + value;
    }
}
