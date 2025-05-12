package com.lat.be.domain.response;

import com.lat.be.domain.Order;
import com.lat.be.domain.User;
import com.lat.be.util.constant.OrderStatus;
import com.lat.be.util.constant.PaymentMethod;
import com.lat.be.util.constant.PaymentStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderWithItemsDTO {
    private Long id;
    private long totalPrice;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private OrderStatus orderStatus;
    private String paymentUrl;
    private String transactionNo;
    private String paymentMessage;
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private String updatedBy;
    private UserDTO user;
    private String phone;
    private String address;
    private List<OrderItemDTO> items;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserDTO {
        private Long id;
        private String email;
        private String name;
        private String phone;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemDTO {
        private Long id;
        private int quantity;
        private long price;
        private long totalPrice;
        private ProductDTO product;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductDTO {
        private Long id;
        private String name;
        private String image;
    }
    
    public static OrderWithItemsDTO fromOrder(Order order) {
        OrderWithItemsDTO dto = OrderWithItemsDTO.builder()
                .id(order.getId())
                .totalPrice(order.getTotalPrice())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .orderStatus(order.getOrderStatus())
                .paymentUrl(order.getPaymentUrl())
                .transactionNo(order.getTransactionNo())
                .paymentMessage(order.getPaymentMessage())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .createdBy(order.getCreatedBy())
                .updatedBy(order.getUpdatedBy())
                .phone(order.getPhone())
                .address(order.getAddress())
                .build();
        
        if (order.getUser() != null) {
            dto.setUser(UserDTO.builder()
                    .id(order.getUser().getId())
                    .email(order.getUser().getEmail())
                    .name(order.getUser().getName())
                    .phone(order.getPhone())
                    .build());
        }
        
        if (order.getOrderDetails() != null) {
            dto.setItems(order.getOrderDetails().stream()
                    .map(detail -> OrderItemDTO.builder()
                            .id(detail.getId())
                            .quantity(detail.getQuantity())
                            .price(detail.getPrice())
                            .totalPrice(detail.getTotalPrice())
                            .product(ProductDTO.builder()
                                    .id(detail.getProduct().getId())
                                    .name(detail.getProduct().getName())
                                    .image(detail.getProduct().getImage())
                                    .build())
                            .build())
                    .toList());
        }
        
        return dto;
    }
} 