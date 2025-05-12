package com.lat.be.domain.request;

import com.lat.be.util.constant.OrderStatus;
import lombok.Data;

@Data
public class UpdateOrderStatus {
    private OrderStatus orderStatus;
}
