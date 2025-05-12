package com.lat.be.controller;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import com.lat.be.domain.Order;
import com.lat.be.service.OrderService;
import com.lat.be.service.VNPayService;
import com.lat.be.util.annotation.ApiMessage;
import com.lat.be.util.constant.PaymentMethod;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {
    private final VNPayService vnPayService;
    private final OrderService orderService;
    
    @Value("${frontend.payment-result-url:http://localhost:3000/payment-result}")
    private String frontendPaymentResultUrl;

    @GetMapping("/create-payment/{orderId}")
    @ApiMessage("Tạo URL thanh toán cho đơn hàng thành công")
    public ResponseEntity<Map<String, String>> createPayment(@PathVariable Long orderId, HttpServletRequest request) {
        Optional<Order> orderOpt = this.orderService.getOrderById(orderId);
        if (orderOpt.isEmpty()) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Không tìm thấy đơn hàng");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        Order order = orderOpt.get();
        // Only allow TRANSFER payment method
        if (order.getPaymentMethod() != PaymentMethod.TRANSFER) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Phương thức thanh toán không hợp lệ. Chỉ chấp nhận TRANSFER");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        // Lấy IP của client
        String ipAddress = getClientIpAddress(request);

        // Generate payment URL
        String orderInfo = "Thanh toan don hang: " + orderId;
        Long totalPrice = (order.getTotalPrice() * 26000);
        Long roundedTotalPrice = (long) (Math.ceil(totalPrice / 10000.0) * 10000);
        System.out.println(roundedTotalPrice);

        String paymentUrl = this.vnPayService.createPaymentUrl(orderId,roundedTotalPrice , orderInfo, ipAddress);

        // Update order with payment URL
        order.setPaymentUrl(paymentUrl);
        orderService.updateOrder(order);

        // Return payment URL
        Map<String, String> response = new HashMap<>();
        response.put("paymentUrl", paymentUrl);
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy địa chỉ IP của client từ request
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        
        // Trong trường hợp client đang chạy trong proxy hoặc có nhiều địa chỉ IP
        if (ipAddress != null && ipAddress.contains(",")) {
            // Lấy địa chỉ IP đầu tiên
            ipAddress = ipAddress.split(",")[0].trim();
        }
        
        return ipAddress;
    }

    @GetMapping("/vnpay-return")
    public RedirectView vnpayReturn(@RequestParam Map<String, String> params) {
        log.info("VNPay return with params: {}", params);
        
        // Process VNPay return
        Optional<Order> processedOrder = vnPayService.processPaymentReturn(params);
        
        // Redirect URL depends on payment result
        String redirectUrl;
        
        if (processedOrder.isPresent()) {
            // Payment successful
            Order order = processedOrder.get();
            String message = "";
            
            // Add payment message to redirect URL if available
            if (order.getPaymentMessage() != null && !order.getPaymentMessage().isEmpty()) {
                try {
                    message = "&message=" + URLEncoder.encode(order.getPaymentMessage(), StandardCharsets.UTF_8.toString());
                } catch (UnsupportedEncodingException e) {
                    log.error("Error encoding payment message", e);
                }
            }
            
            redirectUrl = frontendPaymentResultUrl + "?status=success&orderId=" + order.getId() + message;
        } else {
            // Payment failed
            String orderId = params.getOrDefault("vnp_TxnRef", "unknown");
            String responseCode = params.getOrDefault("vnp_ResponseCode", "99");
            String errorMessage;
            
            // Xử lý thông báo lỗi dựa vào mã lỗi từ VNPay
            switch (responseCode) {
                case "07":
                    errorMessage = "Trừ tiền thành công. Giao dịch bị nghi ngờ (liên quan tới lừa đảo, giao dịch bất thường)";
                    break;
                case "09":
                    errorMessage = "Thẻ/Tài khoản chưa đăng ký dịch vụ InternetBanking";
                    break;
                case "10":
                    errorMessage = "Xác thực thông tin thẻ/tài khoản không đúng quá 3 lần";
                    break;
                case "11":
                    errorMessage = "Đã hết hạn chờ thanh toán. Xin vui lòng thực hiện lại giao dịch";
                    break;
                case "12":
                    errorMessage = "Thẻ/Tài khoản bị khóa";
                    break;
                case "13":
                    errorMessage = "Nhập sai mật khẩu xác thực giao dịch (OTP)";
                    break;
                case "24":
                    errorMessage = "Giao dịch đã được hủy";
                    break;
                case "51":
                    errorMessage = "Tài khoản không đủ số dư để thực hiện giao dịch";
                    break;
                case "65":
                    errorMessage = "Tài khoản vượt quá hạn mức giao dịch trong ngày";
                    break;
                case "75":
                    errorMessage = "Ngân hàng thanh toán đang bảo trì";
                    break;
                case "79":
                    errorMessage = "Nhập sai mật khẩu thanh toán quá số lần quy định";
                    break;
                case "99":
                    errorMessage = "Lỗi không xác định";
                    break;
                default:
                    errorMessage = "Thanh toán không thành công. Mã lỗi: " + responseCode;
            }
            
            try {
                redirectUrl = frontendPaymentResultUrl + "?status=failed&orderId=" + orderId + 
                        "&message=" + URLEncoder.encode(errorMessage, StandardCharsets.UTF_8.toString());
            } catch (UnsupportedEncodingException e) {
                redirectUrl = frontendPaymentResultUrl + "?status=failed&orderId=" + orderId;
                log.error("Error encoding error message", e);
            }
        }
        
        return new RedirectView(redirectUrl);
    }
    
    /**
     * API trả về thông tin thanh toán của đơn hàng
     */
    @GetMapping("/payment-info/{orderId}")
    @ApiMessage("Lấy thông tin thanh toán đơn hàng thành công")
    public ResponseEntity<Map<String, Object>> getPaymentInfo(@PathVariable Long orderId) {
        Optional<Order> orderOpt = orderService.getOrderById(orderId);
        if (orderOpt.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Không tìm thấy đơn hàng");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
        
        Order order = orderOpt.get();
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("orderId", order.getId());
        response.put("paymentStatus", order.getPaymentStatus());
        response.put("paymentMethod", order.getPaymentMethod());
        response.put("paymentMessage", order.getPaymentMessage());
        response.put("totalPrice", order.getTotalPrice());
        response.put("transactionNo", order.getTransactionNo());
        
        return ResponseEntity.ok(response);
    }
} 