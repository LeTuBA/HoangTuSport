package com.lat.be.service;

import com.lat.be.domain.Cart;
import com.lat.be.domain.CartDetail;
import com.lat.be.domain.Product;
import com.lat.be.domain.User;
import com.lat.be.repository.CartDetailRepository;
import com.lat.be.repository.CartRepository;
import com.lat.be.repository.ProductRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CartService {
    private final CartRepository cartRepository;
    private final CartDetailRepository cartDetailRepository;
    private final ProductRepository productRepository;
    private final UserService userService;

    @Transactional
    public Cart getCurrentUserCart() {
        User currentUser = this.userService.getCurrentUser();
        Optional<Cart> userCart = cartRepository.findByUser(currentUser);
        
        if (!userCart.isPresent()) {
            // Tạo giỏ hàng mới nếu người dùng chưa có
            Cart newCart = Cart.builder()
                    .user(currentUser)
                    .build();
            return cartRepository.save(newCart);
        }
        
        return userCart.get();
    }

    @Transactional
    public CartDetail addProductToCart(Long productId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Số lượng sản phẩm phải lớn hơn 0");
        }
        
        // Lấy giỏ hàng hiện tại
        Cart cart = getCurrentUserCart();
        
        // Lấy thông tin sản phẩm
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sản phẩm với ID: " + productId));
        
        // Kiểm tra số lượng tồn kho
        if (product.getQuantity() < quantity) {
            throw new IllegalArgumentException("Số lượng sản phẩm trong kho không đủ");
        }
        
        // Kiểm tra xem sản phẩm đã có trong giỏ hàng chưa
        CartDetail existingItem = cartDetailRepository.findByCartAndProduct(cart, product);
        
        if (existingItem != null) {
            // Nếu sản phẩm đã có trong giỏ hàng, cập nhật số lượng
            existingItem.setQuantity(existingItem.getQuantity() + quantity);
            return cartDetailRepository.save(existingItem);
        } else {
            // Nếu sản phẩm chưa có trong giỏ hàng, thêm mới
            CartDetail newDetail = CartDetail.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(quantity)
                    .build();
            return cartDetailRepository.save(newDetail);
        }
    }

    @Transactional
    public CartDetail updateCartItemQuantity(Long cartDetailId, int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("Số lượng sản phẩm không thể âm");
        }
        
        CartDetail cartDetail = this.cartDetailRepository.findById(cartDetailId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy chi tiết giỏ hàng với ID: " + cartDetailId));
        
        // Kiểm tra số lượng tồn kho
        if (cartDetail.getProduct().getQuantity() < quantity) {
            throw new IllegalArgumentException("Số lượng sản phẩm trong kho không đủ");
        }
        
        if (quantity == 0) {
            // Nếu số lượng = 0, xóa sản phẩm khỏi giỏ hàng
            this.cartDetailRepository.delete(cartDetail);
            return null;
        } else {
            // Cập nhật số lượng
            cartDetail.setQuantity(quantity);
            return cartDetailRepository.save(cartDetail);
        }
    }

    @Transactional
    public void removeCartItem(Long cartDetailId) {
        CartDetail cartDetail = cartDetailRepository.findById(cartDetailId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy chi tiết giỏ hàng với ID: " + cartDetailId));
        
        this.cartDetailRepository.delete(cartDetail);
    }

    public List<CartDetail> getCartItems() {
        Cart cart = getCurrentUserCart();
        return this.cartDetailRepository.findByCart(cart);
    }

    @Transactional
    public void clearCart() {
        Cart cart = getCurrentUserCart();
        List<CartDetail> cartDetails = cartDetailRepository.findByCart(cart);
        this.cartDetailRepository.deleteAll(cartDetails);
    }
} 