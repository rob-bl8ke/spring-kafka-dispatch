package com.cyg.dispatch.handler;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cyg.dispatch.message.OrderCreated;
import com.cyg.dispatch.service.DispatchService;
import com.cyg.dispatch.util.TestEventData;

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
        OrderCreated payload = TestEventData.buildOrderCreatedEvent(UUID.randomUUID(), UUID.randomUUID().toString());
        handler.listen(payload);
        // Add assertions or verifications as needed
        verify(dispatchServiceMock, times(1)).process(payload);
    }
}
