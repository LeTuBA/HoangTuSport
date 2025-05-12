package com.lat.be.service;

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
            
            // Tùy chỉnh thông báo lỗi dựa trên mã lỗi
            String errorMessage;
            switch (vnpResponseCode) {
                case "07":
                    errorMessage = "Trừ tiền thành công. Giao dịch bị nghi ngờ gian lận";
                    break;
                case "09":
                    errorMessage = "Thẻ/Tài khoản chưa đăng ký dịch vụ InternetBanking";
                    break;
                case "10":
                    errorMessage = "Xác thực thông tin thẻ/tài khoản không đúng quá 3 lần";
                    break;
                case "11":
                    errorMessage = "Đã hết hạn chờ thanh toán";
                    break;
                case "12":
                    errorMessage = "Thẻ/Tài khoản bị khóa";
                    break;
                case "13":
                    errorMessage = "Nhập sai mật khẩu xác thực (OTP)";
                    break;
                case "24":
                    errorMessage = "Giao dịch đã bị hủy";
                    break;
                case "51":
                    errorMessage = "Tài khoản không đủ số dư";
                    break;
                case "65":
                    errorMessage = "Tài khoản vượt quá hạn mức giao dịch";
                    break;
                case "75":
                    errorMessage = "Ngân hàng đang bảo trì";
                    break;
                case "79":
                    errorMessage = "Nhập sai mật khẩu thanh toán quá số lần quy định";
                    break;
                default:
                    errorMessage = "Thanh toán không thành công. Mã lỗi: " + vnpResponseCode;
            }
            
            order.setPaymentMessage(errorMessage);
            orderRepository.save(order);
            log.error("Payment failed for order: {} with response code: {}", orderId, vnpResponseCode);
            return Optional.empty();
        }
    }
} 