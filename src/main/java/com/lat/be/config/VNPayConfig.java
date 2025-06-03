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

    @Value("${vnpay.return-url:https://hoangtusport.id.vn/confirmation}")
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
    
    public String createPaymentUrl(Long orderId, String vnpTxnRef, long amount, String orderInfo, String ipAddress) {
        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnpVersion);
        vnp_Params.put("vnp_Command", vnpCommand);
        vnp_Params.put("vnp_TmnCode", vnpTmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amount * 100));
        vnp_Params.put("vnp_CurrCode", "VND");
        String safeOrderInfo = "Thanh toan don hang " + orderId;
        vnp_Params.put("vnp_TxnRef", vnpTxnRef);
        vnp_Params.put("vnp_OrderInfo", safeOrderInfo);
        vnp_Params.put("vnp_OrderType", "200000");
        vnp_Params.put("vnp_Locale", "vn");
        String fullReturnUrl = vnpReturnUrl + "/" + orderId;
        vnp_Params.put("vnp_ReturnUrl", fullReturnUrl);
        vnp_Params.put("vnp_IpAddr", ipAddress);
        ZoneId vietnamZone = ZoneId.of("Asia/Ho_Chi_Minh");
        LocalDateTime vietnamNow = LocalDateTime.now(vietnamZone);
        String vnp_CreateDate = vietnamNow.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);
        LocalDateTime expireTime = vietnamNow.plusMinutes(15);
        String vnp_ExpireDate = expireTime.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);
        StringBuilder query = new StringBuilder();
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
        if (vnpPayUrl == null || vnpPayUrl.isEmpty()) {
            vnpPayUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
        }
        String fullUrl = vnpPayUrl + "?" + queryUrl;
        System.out.println("VNPay URL (FULL): " + fullUrl);
        return fullUrl;
    }
    
    public String createPaymentUrl(Long orderId, long amount, String orderInfo) {
        return createPaymentUrl(orderId, orderId.toString(), amount, orderInfo, vnpIpAddr);
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