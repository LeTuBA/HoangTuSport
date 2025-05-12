package com.lat.be.domain.response;

import com.lat.be.domain.Order;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderResponse {
    private Order order;
    private String paymentUrl;
    private String confirmationUrl;
}
