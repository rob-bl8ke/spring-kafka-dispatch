package com.cyg.dispatch;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import com.cyg.dispatch.message.OrderCreated;
import com.cyg.dispatch.message.OrderDispatched;
import com.cyg.dispatch.service.DispatchService;
import com.cyg.dispatch.util.TestEventData;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.hamcrest.MatcherAssert.assertThat;

public class DispatchServiceTests {

    private DispatchService service;
    private KafkaTemplate kafkaProducerMock;

    @BeforeEach
    void setUp() {
        kafkaProducerMock = mock(KafkaTemplate.class);
        service = new DispatchService(kafkaProducerMock);
    }

    @Test
    void process_Success() throws Exception {
        when(kafkaProducerMock.send(anyString(), any(OrderDispatched.class))).thenReturn(mock(CompletableFuture.class));

        OrderCreated orderCreated = TestEventData.buildOrderCreatedEvent(UUID.randomUUID(), UUID.randomUUID().toString());
        service.process(orderCreated);

        verify(kafkaProducerMock, times(1)).send(eq("order.dispatched"), any(OrderDispatched.class));
    }

    @Test
    void process_ProducerThrowsException() {
        OrderCreated testEvent = TestEventData.buildOrderCreatedEvent(UUID.randomUUID(), UUID.randomUUID().toString());
        doThrow(new RuntimeException("Producer Failure")).when(kafkaProducerMock).send(eq("order.dispatched"), any(OrderDispatched.class));

        Exception exception = assertThrows(RuntimeException.class, () -> service.process(testEvent));

        verify(kafkaProducerMock, times(1)).send(eq("order.dispatched"), any(OrderDispatched.class));
        assertThat(exception.getMessage(), equalTo("Producer Failure"));
    }
}
