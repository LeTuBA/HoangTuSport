package com.lat.be.domain.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductStatDTO {
    Long productId;
    String productName;
    String productImage;
    Long totalQuantitySold;
    Long totalRevenue;
} 