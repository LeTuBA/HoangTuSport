package com.lat.be.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.lat.be.domain.response.GrowthDTO;
import com.lat.be.domain.response.ProductStatDTO;
import com.lat.be.domain.response.StatisticsDTO;
import com.lat.be.repository.OrderDetailRepository;
import com.lat.be.repository.OrderRepository;
import com.lat.be.util.constant.PaymentStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final OrderDetailRepository orderDetailRepository;
    private final OrderRepository orderRepository;

    /**
     * Lấy danh sách 3 sản phẩm bán chạy nhất
     */
    public List<ProductStatDTO> getTopSellingProducts() {
        List<Object[]> results = orderDetailRepository.findTopSellingProducts();
        List<ProductStatDTO> topProducts = new ArrayList<>();
        
        int count = 0;
        for (Object[] result : results) {
            if (count >= 3) break;
            
            ProductStatDTO productStat = ProductStatDTO.builder()
                .productId(result[0] != null ? ((Number) result[0]).longValue() : null)
                .productName((String) result[1])
                .productImage((String) result[2])
                .totalQuantitySold(result[3] != null ? ((Number) result[3]).longValue() : 0L)
                .totalRevenue(result[4] != null ? ((Number) result[4]).longValue() : 0L)
                .build();
            
            topProducts.add(productStat);
            count++;
        }
        
        return topProducts;
    }

    /**
     * Lấy thống kê hiệu suất theo khoảng thời gian
     * @param period Khoảng thời gian (today, week, month, year, all)
     */
    public StatisticsDTO getPerformanceStatistics(String period) {
        Instant startDate = getStartDateForPeriod(period);
        Instant previousStartDate = getPreviousPeriodStartDate(period, startDate);
        
        // Lấy số liệu doanh thu
        Long currentRevenue = orderRepository.sumRevenueInPeriod(startDate, PaymentStatus.PAID);
        Long previousRevenue = orderRepository.sumRevenueInPeriod(previousStartDate, PaymentStatus.PAID);
        GrowthDTO revenueGrowth = calculateGrowth(currentRevenue, previousRevenue);
        
        // Lấy số liệu đơn hàng
        Long currentOrders = orderRepository.countOrdersInPeriod(startDate);
        Long previousOrders = orderRepository.countOrdersInPeriod(previousStartDate);
        GrowthDTO ordersGrowth = calculateGrowth(currentOrders, previousOrders);
        
        // Lấy số liệu sản phẩm đã bán
        Long currentProductsSold = orderDetailRepository.countProductsSoldInPeriod(startDate);
        Long previousProductsSold = orderDetailRepository.countProductsSoldInPeriod(previousStartDate);
        GrowthDTO productsSoldGrowth = calculateGrowth(currentProductsSold, previousProductsSold);
        
        // Lấy số liệu khách hàng
        Long currentCustomers = orderRepository.countCustomersInPeriod(startDate);
        Long previousCustomers = orderRepository.countCustomersInPeriod(previousStartDate);
        GrowthDTO customersGrowth = calculateGrowth(currentCustomers, previousCustomers);
        
        return StatisticsDTO.builder()
                .revenue(revenueGrowth)
                .orders(ordersGrowth)
                .productsSold(productsSoldGrowth)
                .customers(customersGrowth)
                .build();
    }
    
    /**
     * Tính toán tỷ lệ tăng trưởng
     */
    private GrowthDTO calculateGrowth(Long currentValue, Long previousValue) {
        currentValue = Objects.requireNonNullElse(currentValue, 0L);
        previousValue = Objects.requireNonNullElse(previousValue, 0L);
        
        Double growthRate = 0.0;
        if (previousValue > 0) {
            growthRate = ((double) (currentValue - previousValue) / previousValue) * 100;
        } else if (currentValue > 0) {
            growthRate = 100.0; // Nếu trước đó là 0, và hiện tại > 0, tăng trưởng 100%
        }
        
        return GrowthDTO.builder()
                .currentValue(currentValue)
                .previousValue(previousValue)
                .growthRate(growthRate)
                .build();
    }
    
    /**
     * Lấy thời điểm bắt đầu dựa trên khoảng thời gian
     */
    private Instant getStartDateForPeriod(String period) {
        LocalDate today = LocalDate.now();
        ZoneId zoneId = ZoneId.systemDefault();
        
        switch (period.toLowerCase()) {
            case "today":
                return today.atStartOfDay(zoneId).toInstant();
            case "week":
                return today.minusDays(7).atStartOfDay(zoneId).toInstant();
            case "month":
                return today.minusDays(30).atStartOfDay(zoneId).toInstant();
            case "year":
                return today.minusDays(365).atStartOfDay(zoneId).toInstant();
            case "all":
            default:
                return Instant.EPOCH; // Từ đầu thời gian
        }
    }
    
    /**
     * Lấy thời điểm bắt đầu của khoảng thời gian trước đó
     */
    private Instant getPreviousPeriodStartDate(String period, Instant currentStartDate) {
        LocalDate startLocalDate = LocalDate.ofInstant(currentStartDate, ZoneId.systemDefault());
        
        switch (period.toLowerCase()) {
            case "today":
                return startLocalDate.minusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
            case "week":
                return startLocalDate.minusDays(7).atStartOfDay(ZoneId.systemDefault()).toInstant();
            case "month":
                return startLocalDate.minusDays(30).atStartOfDay(ZoneId.systemDefault()).toInstant();
            case "year":
                return startLocalDate.minusDays(365).atStartOfDay(ZoneId.systemDefault()).toInstant();
            case "all":
            default:
                return Instant.EPOCH;
        }
    }
} 