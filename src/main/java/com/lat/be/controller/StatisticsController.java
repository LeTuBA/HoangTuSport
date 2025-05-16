package com.lat.be.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.lat.be.domain.RestResponse;
import com.lat.be.domain.response.ProductStatDTO;
import com.lat.be.domain.response.StatisticsDTO;
import com.lat.be.domain.response.SupplierRevenueResponseDTO;
import com.lat.be.service.RevenueService;
import com.lat.be.service.StatisticsService;

import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;
    private final RevenueService revenueService;

    /**
     * API lấy ra 3 sản phẩm bán chạy nhất
     */
    @GetMapping("/top-selling-products")
    public ResponseEntity<RestResponse<List<ProductStatDTO>>> getTopSellingProducts() {
        List<ProductStatDTO> topProducts = statisticsService.getTopSellingProducts();
        RestResponse<List<ProductStatDTO>> response = new RestResponse<>();
        response.setStatusCode(HttpStatus.OK.value());
        response.setError(null);
        response.setMessage("Lấy danh sách sản phẩm bán chạy thành công");
        response.setData(topProducts);
        return ResponseEntity.ok(response);
    }

    /**
     * API lấy thông tin thống kê hiệu suất theo khoảng thời gian
     * @param period Khoảng thời gian (today, week, month, year, all)
     */
    @GetMapping("/performance/{period}")
    public ResponseEntity<RestResponse<StatisticsDTO>> getPerformanceStatistics(@PathVariable String period) {
        // Validate period
        if (!isValidPeriod(period)) {
            RestResponse<StatisticsDTO> errorResponse = new RestResponse<>();
            errorResponse.setStatusCode(HttpStatus.BAD_REQUEST.value());
            errorResponse.setError("Khoảng thời gian không hợp lệ");
            errorResponse.setMessage("Giá trị hợp lệ: today, week, month, year, all");
            errorResponse.setData(null);
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        StatisticsDTO statistics = statisticsService.getPerformanceStatistics(period);
        RestResponse<StatisticsDTO> response = new RestResponse<>();
        response.setStatusCode(HttpStatus.OK.value());
        response.setError(null);
        response.setMessage("Lấy thông tin thống kê thành công");
        response.setData(statistics);
        return ResponseEntity.ok(response);
    }
    
    /**
     * API lấy thông tin doanh thu theo nhà cung cấp và khoảng thời gian
     * @param period Khoảng thời gian (today, week, month, year, all)
     */
    @GetMapping("/supplier-revenue/{period}")
    public ResponseEntity<RestResponse<SupplierRevenueResponseDTO>> getSupplierRevenue(@PathVariable String period) {
        // Validate period
        if (!isValidPeriod(period)) {
            RestResponse<SupplierRevenueResponseDTO> errorResponse = new RestResponse<>();
            errorResponse.setStatusCode(HttpStatus.BAD_REQUEST.value());
            errorResponse.setError("Khoảng thời gian không hợp lệ");
            errorResponse.setMessage("Giá trị hợp lệ: today, week, month, year, all");
            errorResponse.setData(null);
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        SupplierRevenueResponseDTO supplierRevenue;
        
        // Nếu period là "all", lấy tất cả doanh thu
        if (period.equalsIgnoreCase("all")) {
            supplierRevenue = this.revenueService.getSupplierRevenues();
        } else {
            // Xác định khoảng thời gian dựa trên period
            LocalDateTime endDateTime = LocalDateTime.now();
            LocalDateTime startDateTime;
            
            switch (period.toLowerCase()) {
                case "today":
                    startDateTime = endDateTime.toLocalDate().atStartOfDay();
                    break;
                case "week":
                    startDateTime = endDateTime.toLocalDate().atStartOfDay().minus(7, ChronoUnit.DAYS);
                    break;
                case "month":
                    startDateTime = endDateTime.toLocalDate().atStartOfDay().minus(30, ChronoUnit.DAYS);
                    break;
                case "year":
                    startDateTime = endDateTime.toLocalDate().atStartOfDay().minus(365, ChronoUnit.DAYS);
                    break;
                default:
                    startDateTime = endDateTime.toLocalDate().atStartOfDay();
            }
            
            supplierRevenue = revenueService.getSupplierRevenuesInPeriod(startDateTime, endDateTime);
        }
        
        RestResponse<SupplierRevenueResponseDTO> response = new RestResponse<>();
        response.setStatusCode(HttpStatus.OK.value());
        response.setError(null);
        response.setMessage("Lấy thông tin doanh thu nhà cung cấp thành công");
        response.setData(supplierRevenue);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Kiểm tra khoảng thời gian có hợp lệ hay không
     */
    private boolean isValidPeriod(String period) {
        return period != null && (
            period.equalsIgnoreCase("today") || 
            period.equalsIgnoreCase("week") || 
            period.equalsIgnoreCase("month") || 
            period.equalsIgnoreCase("year") || 
            period.equalsIgnoreCase("all")
        );
    }
} 