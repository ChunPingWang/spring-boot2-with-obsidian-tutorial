package com.example.tutorial.aop;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class MonitoringRecorder {

    private final List<String> methodNames = new ArrayList<>();

    public void record(String methodName) {
        methodNames.add(methodName);
    }

    public List<String> getMethodNames() {
        return Collections.unmodifiableList(methodNames);
    }

    public void reset() {
        methodNames.clear();
    }
}
