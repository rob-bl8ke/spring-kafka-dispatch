package com.cyg.dispatch.service;

import org.springframework.stereotype.Service;

import com.cyg.dispatch.message.OrderCreated;

// It's good pratice to separate the service from the Kafka listener 
// and will help with testing and maintainability.
// The service can be injected into the Kafka listener and used to handle the business logic.
@Service
public class DispatchService {
    public void process(OrderCreated payload) {
        // Business logic to process the order
        // For example, dispatching the order to a delivery service
        System.out.println("Processing order: " + payload);
    }
}
