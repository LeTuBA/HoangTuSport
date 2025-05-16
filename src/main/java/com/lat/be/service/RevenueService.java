package com.lat.be.service;

import com.lat.be.domain.response.SupplierRevenueDTO;
import com.lat.be.domain.response.SupplierRevenueResponseDTO;
import com.lat.be.repository.OrderDetailRepository;
import com.lat.be.util.constant.PaymentStatus;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RevenueService {

    private final OrderDetailRepository orderDetailRepository;

    public SupplierRevenueResponseDTO getSupplierRevenues() {
        List<Object[]> supplierRevenueData = orderDetailRepository.getSupplierRevenues(PaymentStatus.PAID);
        Long totalRevenue = orderDetailRepository.getTotalRevenue(PaymentStatus.PAID);
        
        if (totalRevenue == null) {
            totalRevenue = 0L;
        }
        
        List<SupplierRevenueDTO> data = new ArrayList<>();
        
        for (Object[] result : supplierRevenueData) {
            String name = (String) result[0];
            Long value = ((Number) result[1]).longValue();
            int percentage = totalRevenue > 0 ? (int) (value * 100 / totalRevenue) : 0;
            
            data.add(SupplierRevenueDTO.builder()
                    .name(name)
                    .value(value)
                    .percentage(percentage)
                    .build());
        }
        
        return SupplierRevenueResponseDTO.builder()
                .data(data)
                .totalRevenue(totalRevenue)
                .build();
    }
    
    public SupplierRevenueResponseDTO getSupplierRevenuesInPeriod(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        Instant startDate = startDateTime.atZone(ZoneId.systemDefault()).toInstant();
        Instant endDate = endDateTime.atZone(ZoneId.systemDefault()).toInstant();
        
        List<Object[]> supplierRevenueData = orderDetailRepository.getSupplierRevenuesInPeriod(startDate, endDate, PaymentStatus.PAID);
        Long totalRevenue = orderDetailRepository.getTotalRevenueInPeriod(startDate, endDate, PaymentStatus.PAID);
        
        if (totalRevenue == null) {
            totalRevenue = 0L;
        }
        
        List<SupplierRevenueDTO> data = new ArrayList<>();
        
        for (Object[] result : supplierRevenueData) {
            String name = (String) result[0];
            Long value = ((Number) result[1]).longValue();
            int percentage = totalRevenue > 0 ? (int) (value * 100 / totalRevenue) : 0;
            
            data.add(SupplierRevenueDTO.builder()
                    .name(name)
                    .value(value)
                    .percentage(percentage)
                    .build());
        }
        
        return SupplierRevenueResponseDTO.builder()
                .data(data)
                .totalRevenue(totalRevenue)
                .build();
    }
} 