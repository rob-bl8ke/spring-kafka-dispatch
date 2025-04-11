package com.cyg.dispatch.handler;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cyg.dispatch.service.DispatchService;

public class OrderCreatedHandlerTest {

    private OrderCreatedHandler handler;
    private DispatchService dispatchServiceMock; // Mock this service
    
    @BeforeEach
    void setUp() {
        // Initialize any necessary components or mocks here
        dispatchServiceMock = mock(DispatchService.class);
        // Pass the mock to the handler
        handler = new OrderCreatedHandler(dispatchServiceMock);
    }

    @Test
    void listen() {
        // Simulate the Kafka message consumption and test the handler's behavior
        // You can use mocking frameworks like Mockito to mock dependencies
        // and verify interactions with the DispatchService
        handler.listen("payload");
        // Add assertions or verifications as needed
        verify(dispatchServiceMock, times(1)).process("payload");
    }
}
