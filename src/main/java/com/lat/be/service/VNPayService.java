package com.lat.be.service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.lat.be.config.VNPayConfig;
import com.lat.be.domain.Order;
import com.lat.be.repository.OrderRepository;
import com.lat.be.util.constant.PaymentStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class VNPayService {
    private final VNPayConfig vnPayConfig;
    private final OrderRepository orderRepository;

    public String createPaymentUrl(Long orderId, long amount, String orderInfo) {
        return createPaymentUrl(orderId, amount, orderInfo, vnPayConfig.getVnpIpAddr());
    }
    
    public String createPaymentUrl(Long orderId, long amount, String orderInfo, String ipAddress) {
        String vnpTxnRef = orderId + "-" + System.currentTimeMillis();
        return vnPayConfig.createPaymentUrl(orderId, vnpTxnRef, amount, orderInfo, ipAddress);
    }

    /**
     * Cập nhật trạng thái thanh toán đơn hàng dựa trên kết quả từ VNPay
     */
    public Order updateOrderPaymentStatus(Long orderId, String vnpResponseCode, String vnpTransactionStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + orderId));
        
        // Kiểm tra nếu đơn hàng đã được thanh toán rồi thì không cập nhật nữa
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            log.info("Order {} already paid, skipping update", orderId);
            return order;
        }
        
        // Xác định trạng thái thanh toán dựa trên mã phản hồi từ VNPay
        boolean paymentSuccess = "00".equals(vnpResponseCode) && "00".equals(vnpTransactionStatus);
        
        if (paymentSuccess) {
            // Thanh toán thành công
            order.setPaymentStatus(PaymentStatus.PAID);
            order.setPaymentMessage("Thanh toán thành công qua VNPay");
            log.info("Payment successful for order: {}", orderId);
        } else {
            // Thanh toán thất bại hoặc bị hủy
            order.setPaymentStatus(PaymentStatus.FAILED);
            order.setPaymentMessage(getPaymentErrorMessage(vnpResponseCode));
            log.info("Payment failed for order: {} with response code: {}", orderId, vnpResponseCode);
        }
        
        return orderRepository.save(order);
    }
    
    /**
     * Lấy thông báo lỗi dựa trên mã phản hồi từ VNPay
     */
    private String getPaymentErrorMessage(String responseCode) {
        if (responseCode == null) {
            return "Thanh toán thất bại";
        }
        
        switch (responseCode) {
            case "07":
                return "Trừ tiền thành công. Giao dịch bị nghi ngờ gian lận";
            case "09":
                return "Thẻ/Tài khoản chưa đăng ký dịch vụ InternetBanking";
            case "10":
                return "Xác thực thông tin thẻ/tài khoản không đúng quá 3 lần";
            case "11":
                return "Đã hết hạn chờ thanh toán";
            case "12":
                return "Thẻ/Tài khoản bị khóa";
            case "13":
                return "Nhập sai mật khẩu xác thực (OTP)";
            case "24":
                return "Giao dịch đã bị hủy";
            case "51":
                return "Tài khoản không đủ số dư";
            case "65":
                return "Tài khoản vượt quá hạn mức giao dịch";
            case "75":
                return "Ngân hàng đang bảo trì";
            case "79":
                return "Nhập sai mật khẩu thanh toán quá số lần quy định";
            default:
                return "Thanh toán thất bại. Mã lỗi: " + responseCode;
        }
    }

    public Optional<Order> processPaymentReturn(Map<String, String> vnpParams) {
        // Validate VNPay response (kiểm tra chữ ký)
        if (!vnPayConfig.validateReturnData(vnpParams)) {
            log.error("Invalid checksum from VNPay");
            return Optional.empty();
        }

        // Lấy thông tin thanh toán
        String vnpResponseCode = vnpParams.get("vnp_ResponseCode");
        String vnpTxnRef = vnpParams.get("vnp_TxnRef");
        String orderId = vnpTxnRef.split("-")[0]; // lấy orderId gốc
        log.info("Processing VNPay payment return - vnpTxnRef: {}, orderId: {}, Response Code: {}", vnpTxnRef, orderId, vnpResponseCode);

        // Tìm đơn hàng theo orderId
        Optional<Order> orderOpt = orderRepository.findById(Long.parseLong(orderId));
        if (orderOpt.isEmpty()) {
            log.error("Order not found with orderId: {} (from vnpTxnRef: {})", orderId, vnpTxnRef);
            return Optional.empty();
        }
        Order order = orderOpt.get();
        
        // Tránh xử lý trùng lặp đơn hàng đã thanh toán
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            log.info("Order already paid: {}", orderId);
            return Optional.of(order);
        }

        // Kiểm tra kết quả thanh toán
        if ("00".equals(vnpResponseCode)) {
            // Cập nhật trạng thái thanh toán thành công
            order.setPaymentStatus(PaymentStatus.PAID);
            order.setPaymentMessage("Thanh toán thành công " + order.getTotalPrice() + " đồng");
            
            // Lưu mã giao dịch VNPay nếu có
            if (vnpParams.containsKey("vnp_TransactionNo")) {
                order.setTransactionNo(vnpParams.get("vnp_TransactionNo"));
            }
            
            orderRepository.save(order);
            log.info("Payment successful for order: {}", orderId);
            return Optional.of(order);
        } else {
            // Cập nhật trạng thái thanh toán thất bại
            order.setPaymentStatus(PaymentStatus.FAILED);
            order.setPaymentMessage(getPaymentErrorMessage(vnpResponseCode));
            orderRepository.save(order);
            log.error("Payment failed for order: {} with response code: {}", orderId, vnpResponseCode);
            return Optional.empty();
        }
    }
    
    /**
     * Kiểm tra tính hợp lệ của response từ VNPay
     * @param params Map chứa các tham số từ VNPay
     * @return true nếu chữ ký hợp lệ, false nếu không hợp lệ
     */
    public boolean validatePaymentResponse(Map<String, String[]> params) {
        try {
            // Chuyển đổi từ Map<String, String[]> sang Map<String, String>
            Map<String, String> vnpParams = new HashMap<>();
            for (Map.Entry<String, String[]> entry : params.entrySet()) {
                if (entry.getValue() != null && entry.getValue().length > 0) {
                    vnpParams.put(entry.getKey(), entry.getValue()[0]);
                }
            }
            
            // Kiểm tra chữ ký bằng phương thức validateReturnData
            return vnPayConfig.validateReturnData(vnpParams);
        } catch (Exception e) {
            log.error("Error validating VNPay response", e);
            return false;
        }
    }
} 