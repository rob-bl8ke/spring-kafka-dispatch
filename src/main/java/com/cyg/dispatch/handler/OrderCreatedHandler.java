package com.cyg.dispatch.handler;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.cyg.dispatch.service.DispatchService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Component
public class OrderCreatedHandler {
    
    private final DispatchService dispatchService;
    
    @KafkaListener(
        id = "orderConsumerClient", 
        topics = "order.created", 
        groupId = "dispatch.order.created.consumer"
    )
    public void listen(String payload) {
        dispatchService.process(payload);
    }
}
