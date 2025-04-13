package com.cyg.dispatch.service;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.cyg.dispatch.message.OrderCreated;
import com.cyg.dispatch.message.OrderDispatched;

import lombok.RequiredArgsConstructor;

// It's good pratice to separate the service from the Kafka listener 
// and will help with testing and maintainability.
// The service can be injected into the Kafka listener and used to handle the business logic.
@Service
@RequiredArgsConstructor
public class DispatchService {

    private static final String ORDER_DISPATCHED_TOPIC = "order.dispatched";
    private final KafkaTemplate<String, Object> kafkaProducer;

    public void process(OrderCreated orderCreated) throws Exception {
        OrderDispatched orderDispatched = OrderDispatched.builder()
                .orderId(orderCreated.getOrderId())
                .build();

        // By using get() this operation will be synchronous instead of asynchronous.
        kafkaProducer.send(ORDER_DISPATCHED_TOPIC, orderDispatched).get();
    }
}
