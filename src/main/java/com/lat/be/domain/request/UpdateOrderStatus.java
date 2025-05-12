package com.lat.be.domain.request;

import lombok.Data;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrderStatus {
    private String orderStatus;
    private String paymentStatus;
}
