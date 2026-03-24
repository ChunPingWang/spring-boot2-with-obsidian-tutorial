package com.example.modulith.customer;

import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Service
public class CustomerService {

    private final Set<Long> knownCustomers = new HashSet<Long>(Arrays.asList(1L, 2L));

    public boolean exists(Long customerId) {
        return knownCustomers.contains(customerId);
    }
}
