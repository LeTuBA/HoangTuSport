package com.lat.be.controller;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.lat.be.domain.Order;
import com.lat.be.domain.OrderDetail;
import com.lat.be.domain.User;
import com.lat.be.domain.request.CreateOrderDTO;
import com.lat.be.domain.response.OrderResponse;
import com.lat.be.domain.response.ResultPaginationDTO;
import com.lat.be.service.OrderService;
import com.lat.be.service.UserService;
import com.lat.be.service.VNPayService;
import com.lat.be.util.annotation.ApiMessage;
import com.lat.be.util.constant.PaymentMethod;
import com.lat.be.util.constant.PaymentStatus;
import com.turkraft.springfilter.boot.Filter;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;
    private final UserService userService;
    private final VNPayService vnPayService;
    
    @PreAuthorize("hasAnyRole('admin', 'employee', 'user')")
    @PostMapping
    @ApiMessage("Tạo đơn hàng từ giỏ hàng thành công")
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderDTO createOrderDTO) {
        try {
            Order order = orderService.createOrder(createOrderDTO);
            
            OrderResponse response = OrderResponse.builder()
                .order(order)
                .build();

            // Xử lý theo phương thức thanh toán
            if (order.getPaymentMethod() == PaymentMethod.COD) {
                // Nếu là COD, chỉ cần tạo đơn hàng với trạng thái PENDING
                order.setPaymentStatus(PaymentStatus.PENDING);
                order.setPaymentMessage("Cảm ơn bạn đã đặt hàng. Vui lòng chuẩn bị số tiền " + order.getTotalPrice() + " VNĐ khi nhận hàng");
                orderService.updateOrder(order);
            } else if (order.getPaymentMethod() == PaymentMethod.TRANSFER) {
                // Nếu là TRANSFER, tạo URL thanh toán VNPay
                String orderInfo = "Thanh toan don hang: " + order.getId();
                String paymentUrl = vnPayService.createPaymentUrl(
                    order.getId(), 
                    order.getTotalPrice(), 
                    orderInfo
                );
                
                order.setPaymentStatus(PaymentStatus.PENDING);
                order.setPaymentMessage("Vui lòng thanh toán để hoàn tất đơn hàng");
                order.setPaymentUrl(paymentUrl);
                orderService.updateOrder(order);
                
                response.setPaymentUrl(paymentUrl);
            }
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            throw new RuntimeException("Lỗi khi tạo đơn hàng: " + e.getMessage());
        }
    }
    
    @PreAuthorize("hasAnyRole('admin', 'employee')")
    @GetMapping
    @ApiMessage("Lấy danh sách đơn hàng thành công")
    public ResponseEntity<ResultPaginationDTO> getAllOrders(
            @Filter Specification<Order> orderSpec,
            Pageable pageable) {
        
        ResultPaginationDTO result = orderService.getAllOrders(orderSpec, pageable);
        return ResponseEntity.ok(result);
    }
    
    @PreAuthorize("hasAnyRole('admin', 'employee', 'user')")
    @GetMapping("/{id}")
    @ApiMessage("Lấy thông tin đơn hàng thành công")
    public ResponseEntity<Order> getOrderById(@PathVariable("id") Long id) {
        Order order = orderService.getOrderById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + id));
        return ResponseEntity.ok(order);
    }
    
    @GetMapping("/my-orders")
    @PreAuthorize("hasAnyRole('admin', 'employee', 'user')")
    @ApiMessage("Lấy danh sách đơn hàng của tôi thành công")
    public ResponseEntity<List<Order>> getMyOrders() {
        User currentUser = userService.getCurrentUser();
        List<Order> myOrders = orderService.getOrdersByUser(currentUser);
        return ResponseEntity.ok(myOrders);
    }
    
    @GetMapping("/{id}/details")
    @PreAuthorize("hasAnyRole('admin', 'employee', 'user')")
    @ApiMessage("Lấy chi tiết đơn hàng thành công")
    public ResponseEntity<List<OrderDetail>> getOrderDetails(@PathVariable("id") Long id) {
        List<OrderDetail> orderDetails = orderService.getOrderDetails(id);
        return ResponseEntity.ok(orderDetails);
    }
    
    @GetMapping("/{id}/payment-url")
    @PreAuthorize("hasAnyRole('admin', 'employee', 'user')")
    @ApiMessage("Lấy URL thanh toán cho đơn hàng thành công")
    public ResponseEntity<?> getPaymentUrl(@PathVariable("id") Long id) {
        Order order = orderService.getOrderById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + id));
        
        // Chỉ tạo URL thanh toán cho phương thức TRANSFER
        if (order.getPaymentMethod() != PaymentMethod.TRANSFER) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Đơn hàng không sử dụng phương thức thanh toán chuyển khoản");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        
        // Nếu đã có URL thanh toán, trả về URL đó
        if (order.getPaymentUrl() != null && !order.getPaymentUrl().isEmpty()) {
            Map<String, String> response = new HashMap<>();
            response.put("paymentUrl", order.getPaymentUrl());
            return ResponseEntity.ok(response);
        }
        
        // Tạo URL thanh toán mới
        String orderInfo = "Thanh toan don hang: " + order.getId();
        String paymentUrl = vnPayService.createPaymentUrl(order.getId(), order.getTotalPrice(), orderInfo);
        
        // Cập nhật đơn hàng với URL thanh toán
        order.setPaymentUrl(paymentUrl);
        orderService.updateOrder(order);
        
        Map<String, String> response = new HashMap<>();
        response.put("paymentUrl", paymentUrl);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{id}/update-payment-status")
    @PreAuthorize("hasAnyRole('admin', 'employee')")
    @ApiMessage("Cập nhật trạng thái thanh toán thành công")
    public ResponseEntity<?> updatePaymentStatus(
            @PathVariable("id") Long id,
            @RequestParam("paymentStatus") PaymentStatus paymentStatus) {
        
        Order order = orderService.getOrderById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + id));
        
        // Update payment status
        order.setPaymentStatus(paymentStatus);
        
        // Add payment message
        if (paymentStatus == PaymentStatus.PAID) {
            order.setPaymentMessage("Thanh toán thành công");
        } else if (paymentStatus == PaymentStatus.FAILED) {
            order.setPaymentMessage("Thanh toán thất bại");
        } else if (paymentStatus == PaymentStatus.PENDING) {
            order.setPaymentMessage("Đang chờ thanh toán");
        } else if (paymentStatus == PaymentStatus.REFUNDED) {
            order.setPaymentMessage("Đã hoàn tiền");
        }
        
        Order updatedOrder = orderService.updateOrder(order);
        return ResponseEntity.ok(updatedOrder);
    }
} 