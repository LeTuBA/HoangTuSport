package com.lat.be.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lat.be.domain.Order;
import com.lat.be.service.OrderService;
import com.lat.be.service.VNPayService;
import com.lat.be.util.constant.PaymentStatus;
import com.lat.be.util.annotation.ApiMessage;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/vnpay")
@RequiredArgsConstructor
@Slf4j
public class VNPayController {

    private final VNPayService vnPayService;
    private final OrderService orderService;

    /**
     * API xử lý kết quả thanh toán từ frontend
     * Frontend call API này với các tham số nhận được từ VNPay
     */
    @GetMapping("/process-payment")
    @ApiMessage("Xử lý kết quả thanh toán VNPay thành công")
    public ResponseEntity<Map<String, Object>> processPayment(
            @RequestParam String vnp_TxnRef,
            @RequestParam String vnp_ResponseCode,
            @RequestParam String vnp_TransactionStatus,
            @RequestParam(required = false) String vnp_Amount,
            @RequestParam(required = false) String vnp_BankCode,
            @RequestParam(required = false) String vnp_TransactionNo,
            @RequestParam(required = false) String vnp_PayDate,
            @RequestParam String vnp_SecureHash,
            HttpServletRequest request) {
        
        log.info("Processing VNPay payment - TxnRef: {}, ResponseCode: {}", vnp_TxnRef, vnp_ResponseCode);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Kiểm tra tính hợp lệ của chữ ký
            boolean isValidSignature = vnPayService.validatePaymentResponse(request.getParameterMap());
            
            if (!isValidSignature) {
                response.put("success", false);
                response.put("message", "Chữ ký không hợp lệ - có thể bị can thiệp");
                return ResponseEntity.ok(response);
            }
            
            // Lấy orderId từ vnpTxnRef
            Long orderId = extractOrderIdFromTxnRef(vnp_TxnRef);
            if (orderId == null) {
                response.put("success", false);
                response.put("message", "Mã giao dịch không hợp lệ");
                return ResponseEntity.ok(response);
            }
            
            // Cập nhật trạng thái đơn hàng
            Order order = vnPayService.updateOrderPaymentStatus(orderId, vnp_ResponseCode, vnp_TransactionStatus);
            
            // Trả về kết quả đầy đủ
            boolean success = PaymentStatus.PAID.equals(order.getPaymentStatus());
            response.put("success", success);
            response.put("orderId", orderId);
            response.put("paymentStatus", order.getPaymentStatus().name());
            response.put("orderStatus", order.getOrderStatus().name());
            response.put("paymentMessage", order.getPaymentMessage());
            
            // Thông tin giao dịch từ VNPay
            response.put("vnpayInfo", Map.of(
                "amount", vnp_Amount != null ? Long.parseLong(vnp_Amount) / 100 : 0, // VNPay trả về amount * 100
                "bankCode", vnp_BankCode != null ? vnp_BankCode : "",
                "transactionNo", vnp_TransactionNo != null ? vnp_TransactionNo : "",
                "payDate", vnp_PayDate != null ? vnp_PayDate : "",
                "responseCode", vnp_ResponseCode,
                "transactionStatus", vnp_TransactionStatus
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error processing VNPay payment", e);
            response.put("success", false);
            response.put("message", "Lỗi xử lý kết quả thanh toán: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }
    
    /**
     * API kiểm tra trạng thái thanh toán của đơn hàng (không cập nhật gì)
     */
    @GetMapping("/payment-status/{orderId}")
    @ApiMessage("Lấy trạng thái thanh toán đơn hàng thành công")
    public ResponseEntity<Map<String, Object>> getPaymentStatus(@PathVariable Long orderId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Order order = orderService.getOrderById(orderId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + orderId));
            
            response.put("success", true);
            response.put("orderId", orderId);
            response.put("paymentStatus", order.getPaymentStatus().name());
            response.put("orderStatus", order.getOrderStatus().name());
            response.put("paymentMessage", order.getPaymentMessage());
            response.put("paymentUrl", order.getPaymentUrl());
            response.put("totalPrice", order.getTotalPrice());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error getting payment status for order: {}", orderId, e);
            response.put("success", false);
            response.put("message", "Lỗi lấy trạng thái thanh toán: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }
    
    /**
     * Trích xuất orderId từ vnpTxnRef
     */
    private Long extractOrderIdFromTxnRef(String vnpTxnRef) {
        try {
            if (vnpTxnRef == null || vnpTxnRef.isEmpty()) {
                return null;
            }
            
            // vnpTxnRef có format: orderId-timestamp
            String[] parts = vnpTxnRef.split("-");
            if (parts.length > 0) {
                return Long.parseLong(parts[0]);
            }
            
            return null;
        } catch (NumberFormatException e) {
            log.error("Invalid vnpTxnRef format: {}", vnpTxnRef);
            return null;
        }
    }
} 