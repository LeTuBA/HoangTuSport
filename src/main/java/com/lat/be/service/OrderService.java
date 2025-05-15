package com.lat.be.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lat.be.domain.Order;
import com.lat.be.domain.OrderDetail;
import com.lat.be.domain.Product;
import com.lat.be.domain.User;
import com.lat.be.domain.CartDetail;
import com.lat.be.domain.request.CreateOrderDTO;
import com.lat.be.domain.response.ResultPaginationDTO;
import com.lat.be.repository.OrderDetailRepository;
import com.lat.be.repository.OrderRepository;
import com.lat.be.repository.ProductRepository;
import com.lat.be.util.constant.PaymentStatus;
import com.lat.be.util.constant.OrderStatus;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final ProductRepository productRepository;
    private final CartService cartService;
    private final UserService userService;

    @Transactional
    public Order createOrder(CreateOrderDTO createOrderDTO) {
        // Lấy thông tin người dùng hiện tại
        User currentUser = this.userService.getCurrentUser();
        
        // Lấy danh sách sản phẩm từ giỏ hàng
        List<CartDetail> cartItems = this.cartService.getCartItems();
        
        if (cartItems.isEmpty()) {
            throw new RuntimeException("Giỏ hàng của bạn đang trống, không thể tạo đơn hàng");
        }
        
        // Tính tổng giá trị đơn hàng
        long totalPrice = 0;
        for (CartDetail item : cartItems) {
            Product product = item.getProduct();
            long itemPrice = product.getSellPrice() * item.getQuantity();
            totalPrice += itemPrice;
        }
        
        // Tạo và lưu đơn hàng
        Order order = Order.builder()
                .paymentMethod(createOrderDTO.getPaymentMethod())
                .totalPrice(totalPrice)
                .user(currentUser)
                .phone(createOrderDTO.getPhone())
                .address(createOrderDTO.getAddress())
                .paymentStatus(PaymentStatus.PENDING)
                .orderStatus(OrderStatus.PENDING)
                .createdAt(Instant.now())
                .createdBy(currentUser.getEmail())
                .build();
        
        Order savedOrder = orderRepository.save(order);
        
        // Tạo và lưu chi tiết đơn hàng
        List<OrderDetail> orderDetails = new ArrayList<>();
        for (CartDetail item : cartItems) {
            Product product = item.getProduct();
            
            // Cập nhật số lượng tồn kho
            int currentStock = product.getQuantity();
            if (currentStock < item.getQuantity()) {
                throw new RuntimeException("Số lượng sản phẩm " + product.getName() + " không đủ");
            }
            product.setQuantity(currentStock - item.getQuantity());
            productRepository.save(product);
            
            OrderDetail orderDetail = OrderDetail.builder()
                    .order(savedOrder)
                    .product(product)
                    .price(product.getSellPrice())
                    .quantity(item.getQuantity())
                    .totalPrice(product.getSellPrice() * item.getQuantity())
                    .createdAt(Instant.now())
                    .createdBy(currentUser.getEmail())
                    .build();
            
            orderDetails.add(orderDetailRepository.save(orderDetail));
        }
        
        // Xóa giỏ hàng sau khi đặt hàng thành công
        cartService.clearCart();
        
        return savedOrder;
    }

    public Order updateOrder(Order order) {
        return orderRepository.save(order);
    }
    
    public Optional<Order> getOrderById(Long id) {
        return this.orderRepository.findById(id);
    }
    
    public ResultPaginationDTO getAllOrders(Specification<Order> orderSpec, Pageable pageable) {
        int pageNumber = pageable.getPageNumber();
        if (pageNumber > 0) {
            pageNumber = pageNumber - 1;
        }
        
        // Tạo lại pageable với pageNumber mới
        Pageable adjustedPageable = PageRequest.of(pageNumber, pageable.getPageSize(), pageable.getSort());
        
        // Lấy danh sách đơn hàng với pageable đã điều chỉnh
        Page<Order> orderPage = this.orderRepository.findAll(orderSpec, adjustedPageable);
        
        ResultPaginationDTO result = new ResultPaginationDTO();
        ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta();
        meta.setPage(pageNumber + 1); // Trả về số trang theo format của client
        meta.setPageSize(pageable.getPageSize());
        meta.setPages(orderPage.getTotalPages());
        meta.setTotal(orderPage.getTotalElements());
        
        result.setMeta(meta);
        result.setResult(orderPage.getContent());
        
        return result;
    }
    
    public List<Order> getOrdersByUser(User user) {
        return orderRepository.findByUser(user);
    }
    
    /**
     * Lấy danh sách đơn hàng kèm theo thông tin chi tiết về các sản phẩm
     * @param user Người dùng
     * @return Danh sách đơn hàng
     */
    public List<Order> getOrdersWithDetailsByUser(User user) {
        List<Order> orders = orderRepository.findByUser(user);
        
        // Đảm bảo rằng danh sách chi tiết đơn hàng được khởi tạo
        for (Order order : orders) {
            if (order.getOrderDetails() == null || order.getOrderDetails().isEmpty()) {
                order.setOrderDetails(orderDetailRepository.findByOrder(order));
            }
        }
        
        return orders;
    }
    
    public List<OrderDetail> getOrderDetails(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + orderId));
        return order.getOrderDetails();
    }
} 