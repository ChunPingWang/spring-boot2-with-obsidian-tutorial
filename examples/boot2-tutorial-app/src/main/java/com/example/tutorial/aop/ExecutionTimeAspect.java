package com.example.tutorial.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ExecutionTimeAspect {

    private final MonitoringRecorder monitoringRecorder;

    public ExecutionTimeAspect(MonitoringRecorder monitoringRecorder) {
        this.monitoringRecorder = monitoringRecorder;
    }

    @Around("execution(* com.example.tutorial.aop.MonitoredBusinessService.*(..))")
    public Object monitor(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = joinPoint.proceed();
        monitoringRecorder.record(joinPoint.getSignature().getName());
        return result;
    }
}
