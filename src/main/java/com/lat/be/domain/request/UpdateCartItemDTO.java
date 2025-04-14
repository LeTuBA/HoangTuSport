package com.lat.be.domain.request;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class UpdateCartItemDTO {
    @Min(value = 0, message = "Số lượng không được âm")
    private Integer quantity;
} 