package com.lat.be.domain.request;

import com.lat.be.util.constant.PaymentStatus;

import lombok.Data;

@Data
public class UpdatePaymentStatus {
    private PaymentStatus paymentStatus;
}
