package com.lat.be.domain.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GrowthDTO {
    Long currentValue;
    Long previousValue;
    Double growthRate; // Tỷ lệ phần trăm tăng trưởng (có thể âm nếu giảm)
} 