package com.lat.be.domain.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StatisticsDTO {
    GrowthDTO revenue; // Doanh thu
    GrowthDTO orders; // Đơn hàng
    GrowthDTO productsSold; // Sản phẩm đã bán
    GrowthDTO customers; // Khách hàng
} 