package com.cyg.dispatch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cyg.dispatch.message.OrderCreated;
import com.cyg.dispatch.service.DispatchService;

public class DispatchServiceTests {

    private DispatchService service;
    @BeforeEach
    void setUp() {
        service = new DispatchService();
    }

    @Test
    void process() {
        service.process(new OrderCreated());;
    }
}
