package com.cyg.dispatch.handler;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.cyg.dispatch.message.OrderCreated;
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
        groupId = "dispatch.order.created.consumer",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void listen(OrderCreated payload) {
        log.info("Received message: {}", payload);
        try {
            dispatchService.process(payload);
        } catch (Exception e) {
            // Don't throw an exception here.
            // If you throw an exception, and it  isn't handled.consumerFactory
            // the message will be reprocessed and the offset will not be committed.
            // This way, the message is marked as processed and offset is committed.
            // If you want to retry the message, you can use a retry template or a dead letter topic.
            log.error("Processing failure: {}", e.getMessage(), e);
        }
    }
}
