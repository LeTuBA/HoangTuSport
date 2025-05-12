package com.lat.be.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Component
@Configuration
@Getter
@Setter
public class VNPayConfig {
    @Value("${vnpay.pay-url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}")
    private String vnpPayUrl;

    @Value("${vnpay.return-url:http://localhost:8080/api/payment/vnpay-return}")
    private String vnpReturnUrl;

    @Value("${vnpay.tmn-code:YOURCODE}")
    private String vnpTmnCode;

    @Value("${vnpay.hash-secret:YOURHASHSECRET}")
    private String vnpHashSecret;

    @Value("${vnpay.api-url:https://sandbox.vnpayment.vn/merchant_webapi/api/transaction}")
    private String vnpApiUrl;

    @Value("${vnpay.version:2.1.0}")
    private String vnpVersion;

    @Value("${vnpay.command:pay}")
    private String vnpCommand;

    @Value("${vnpay.ip-addr:127.0.0.1}")
    private String vnpIpAddr;

    public String hmacSHA512(String key, String data) {
        try {
            Mac sha512_HMAC = Mac.getInstance("HmacSHA512");
            SecretKeySpec secret_key = new SecretKeySpec(key.getBytes(), "HmacSHA512");
            sha512_HMAC.init(secret_key);
            byte[] hash = sha512_HMAC.doFinal(data.getBytes());
            return bytesToHex(hash);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to calculate hmac-sha512", ex);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder();
        for (byte b : bytes) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }
    
    public String createPaymentUrl(Long orderId, long amount, String orderInfo, String ipAddress) {
        String vnp_TxnRef = orderId.toString();
        
        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnpVersion);
        vnp_Params.put("vnp_Command", vnpCommand);
        vnp_Params.put("vnp_TmnCode", vnpTmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amount * 100)); // Amount in VND, convert to smallest unit (100 đồng)
        vnp_Params.put("vnp_CurrCode", "VND");
        
        // Đảm bảo orderInfo không chứa ký tự đặc biệt, để tránh lỗi định dạng
        String safeOrderInfo = "Thanh toan don hang " + orderId;
        
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", safeOrderInfo);
        vnp_Params.put("vnp_OrderType", "200000"); // Mã cho hàng hóa thương mại điện tử
        vnp_Params.put("vnp_Locale", "vn");
        
        // Tạo ReturnUrl đầy đủ với orderId
        String fullReturnUrl = vnpReturnUrl;
        // Kiểm tra xem return url đã có dấu "/" ở cuối chưa
        if (!fullReturnUrl.endsWith("/")) {
            fullReturnUrl += "/";
        }
        // Thêm orderId vào return url
        fullReturnUrl += orderId;
        
        vnp_Params.put("vnp_ReturnUrl", fullReturnUrl);
        vnp_Params.put("vnp_IpAddr", ipAddress);
        
        // Lấy thời gian hiện tại theo múi giờ Việt Nam (UTC+7)
        ZoneId vietnamZone = ZoneId.of("Asia/Ho_Chi_Minh");
        LocalDateTime vietnamNow = LocalDateTime.now(vietnamZone);
        
        // In ra thời gian hiện tại để debug
        System.out.println("Current time in Vietnam: " + vietnamNow.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        // Chuyển đổi thành định dạng yyyyMMddHHmmss cho VNPay
        String vnp_CreateDate = vietnamNow.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);
        
        // Thêm thời gian hết hạn (15 phút)
        LocalDateTime expireTime = vietnamNow.plusMinutes(15);
        String vnp_ExpireDate = expireTime.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);
        
        // Debug thời gian tạo và hết hạn
        System.out.println("VNPay Create Date: " + vnp_CreateDate + " | Expire Date: " + vnp_ExpireDate);
        
        // Xây dựng chuỗi query và hash
        StringBuilder query = new StringBuilder();
        StringBuilder hashData = new StringBuilder();
        
        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                // Xây dựng hash data
                hashData.append(fieldName);
                hashData.append('=');
                try {
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                    // Xây dựng query
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()));
                    query.append('=');
                    query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException("Error encoding URL parameters", e);
                }
                
                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }
        
        String queryUrl = query.toString();
        String vnp_SecureHash = hmacSHA512(vnpHashSecret, hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
        
        // Đảm bảo vnpPayUrl có giá trị
        if (vnpPayUrl == null || vnpPayUrl.isEmpty()) {
            vnpPayUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
        }
        
        // Tạo URL đầy đủ
        String fullUrl = vnpPayUrl + "?" + queryUrl;
        
        // In ra URL để debug - đảm bảo in đủ URL
        System.out.println("VNPay URL (FULL): " + fullUrl);
        
        return fullUrl;
    }
    
    public String createPaymentUrl(Long orderId, long amount, String orderInfo) {
        return createPaymentUrl(orderId, amount, orderInfo, vnpIpAddr);
    }
    
    public boolean validateReturnData(Map<String, String> vnp_Params) {
        // Remove vnp_SecureHash
        String vnp_SecureHash = vnp_Params.get("vnp_SecureHash");
        vnp_Params.remove("vnp_SecureHash");
        
        // Remove vnp_SecureHashType
        if (vnp_Params.containsKey("vnp_SecureHashType")) {
            vnp_Params.remove("vnp_SecureHashType");
        }
        
        // Build check sum
        StringBuilder hashData = new StringBuilder();
        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                hashData.append(fieldName);
                hashData.append('=');
                try {
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException("Error encoding URL parameters", e);
                }
                
                if (itr.hasNext()) {
                    hashData.append('&');
                }
            }
        }
        
        String checkSum = hmacSHA512(vnpHashSecret, hashData.toString());
        return checkSum.equals(vnp_SecureHash);
    }
} 