package com.lat.be.domain.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierRevenueResponseDTO {
    private List<SupplierRevenueDTO> data;
    private Long totalRevenue;
} 