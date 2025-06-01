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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final OrderDetailRepository orderDetailRepository;
    private final OrderRepository orderRepository;
    
    // Múi giờ UTC+7 (Asia/Ho_Chi_Minh)
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

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
        // Lấy thời gian hiện tại theo múi giờ UTC+7
        LocalDateTime now = LocalDateTime.now(VIETNAM_ZONE);
        
        // Xác định khoảng thời gian hiện tại
        LocalDateTime startDateTime = getStartDateTime(period, now);
        LocalDateTime endDateTime = getEndDateTime(period, now);
        
        // Xác định khoảng thời gian trước đó (cùng độ dài)
        LocalDateTime previousStartDateTime = getPreviousStartDateTime(period, startDateTime);
        LocalDateTime previousEndDateTime = getPreviousEndDateTime(period, startDateTime);
        
        // Chuyển đổi sang Instant
        Instant startDate = startDateTime.atZone(VIETNAM_ZONE).toInstant();
        Instant endDate = endDateTime.atZone(VIETNAM_ZONE).toInstant();
        Instant previousStartDate = previousStartDateTime.atZone(VIETNAM_ZONE).toInstant();
        Instant previousEndDate = previousEndDateTime.atZone(VIETNAM_ZONE).toInstant();
        
        // Lấy số liệu doanh thu
        Long currentRevenue = orderRepository.sumRevenueInPeriod(startDate, endDate, PaymentStatus.PAID);
        Long previousRevenue = orderRepository.sumRevenueInPeriod(previousStartDate, previousEndDate, PaymentStatus.PAID);
        
        // Lấy số liệu đơn hàng
        Long currentOrders = orderRepository.countOrdersInPeriod(startDate, endDate);
        Long previousOrders = orderRepository.countOrdersInPeriod(previousStartDate, previousEndDate);
        
        // Lấy số liệu sản phẩm đã bán
        Long currentProductsSold = orderDetailRepository.countProductsSoldInPeriod(startDate, endDate);
        Long previousProductsSold = orderDetailRepository.countProductsSoldInPeriod(previousStartDate, previousEndDate);
        
        // Lấy số liệu khách hàng
        Long currentCustomers = orderRepository.countCustomersInPeriod(startDate, endDate);
        Long previousCustomers = orderRepository.countCustomersInPeriod(previousStartDate, previousEndDate);
        
        // Tính toán tăng trưởng
        GrowthDTO revenueGrowth = calculateGrowth(currentRevenue, previousRevenue);
        GrowthDTO ordersGrowth = calculateGrowth(currentOrders, previousOrders);
        GrowthDTO productsSoldGrowth = calculateGrowth(currentProductsSold, previousProductsSold);
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
    private LocalDateTime getStartDateTime(String period, LocalDateTime now) {
        switch (period.toLowerCase()) {
            case "today":
                return now.toLocalDate().atStartOfDay();
            case "week":
                // Thứ 2 của tuần hiện tại
                return now.toLocalDate()
                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    .atStartOfDay();
            case "month":
                // Ngày 1 của tháng hiện tại
                return now.toLocalDate()
                    .with(TemporalAdjusters.firstDayOfMonth())
                    .atStartOfDay();
            case "year":
                // Ngày 1/1 của năm hiện tại
                return now.toLocalDate()
                    .with(TemporalAdjusters.firstDayOfYear())
                    .atStartOfDay();
            case "all":
            default:
                return LocalDateTime.of(1970, 1, 1, 0, 0);
        }
    }
    
    /**
     * Lấy thời điểm kết thúc dựa trên khoảng thời gian
     */
    private LocalDateTime getEndDateTime(String period, LocalDateTime now) {
        switch (period.toLowerCase()) {
            case "today":
                return now.toLocalDate().atTime(23, 59, 59);
            case "week":
                // Chủ nhật của tuần hiện tại
                return now.toLocalDate()
                    .with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                    .atTime(23, 59, 59);
            case "month":
                // Ngày cuối tháng hiện tại
                return now.toLocalDate()
                    .with(TemporalAdjusters.lastDayOfMonth())
                    .atTime(23, 59, 59);
            case "year":
                // Ngày 31/12 của năm hiện tại
                return now.toLocalDate()
                    .with(TemporalAdjusters.lastDayOfYear())
                    .atTime(23, 59, 59);
            case "all":
            default:
                return now;
        }
    }
    
    /**
     * Lấy thời điểm bắt đầu của khoảng thời gian trước đó
     */
    private LocalDateTime getPreviousStartDateTime(String period, LocalDateTime startDateTime) {
        switch (period.toLowerCase()) {
            case "today":
                // Hôm qua
                return startDateTime.minusDays(1);
            case "week":
                // Thứ 2 của tuần trước
                return startDateTime.minusWeeks(1);
            case "month":
                // Ngày 1 của tháng trước
                return startDateTime.minusMonths(1);
            case "year":
                // Ngày 1/1 của năm trước
                return startDateTime.minusYears(1);
            case "all":
            default:
                return LocalDateTime.of(1970, 1, 1, 0, 0);
        }
    }
    
    /**
     * Lấy thời điểm kết thúc của khoảng thời gian trước đó
     */
    private LocalDateTime getPreviousEndDateTime(String period, LocalDateTime startDateTime) {
        switch (period.toLowerCase()) {
            case "today":
                // 23:59:59 hôm qua
                return startDateTime.minusDays(1).toLocalDate().atTime(23, 59, 59);
            case "week":
                // Chủ nhật của tuần trước
                return startDateTime.minusWeeks(1).toLocalDate()
                    .with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                    .atTime(23, 59, 59);
            case "month":
                // Ngày cuối tháng trước
                return startDateTime.minusMonths(1).toLocalDate()
                    .with(TemporalAdjusters.lastDayOfMonth())
                    .atTime(23, 59, 59);
            case "year":
                // Ngày 31/12 của năm trước
                return startDateTime.minusYears(1).toLocalDate()
                    .with(TemporalAdjusters.lastDayOfYear())
                    .atTime(23, 59, 59);
            case "all":
            default:
                return LocalDateTime.of(1970, 1, 1, 0, 0);
        }
    }
} 