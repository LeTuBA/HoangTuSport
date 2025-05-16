package com.lat.be.controller;

import com.lat.be.domain.response.SupplierRevenueResponseDTO;
import com.lat.be.service.RevenueService;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/revenue")
@RequiredArgsConstructor
public class RevenueController {

    private final RevenueService revenueService;

    @GetMapping("/suppliers")
    public ResponseEntity<SupplierRevenueResponseDTO> getSupplierRevenues() {
        return ResponseEntity.ok(revenueService.getSupplierRevenues());
    }

    @GetMapping("/suppliers/period")
    public ResponseEntity<SupplierRevenueResponseDTO> getSupplierRevenuesInPeriod(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(revenueService.getSupplierRevenuesInPeriod(startDate, endDate));
    }
} 