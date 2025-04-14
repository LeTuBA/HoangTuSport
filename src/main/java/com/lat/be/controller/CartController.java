package com.lat.be.controller;

import com.lat.be.domain.CartDetail;
import com.lat.be.domain.request.UpdateCartItemDTO;
import com.lat.be.service.CartService;
import com.lat.be.util.annotation.ApiMessage;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/carts")
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;

    @PreAuthorize("hasAnyRole('admin', 'employee', 'user')")
    @PostMapping("/add")
    @ApiMessage("Thêm sản phẩm vào giỏ hàng")
    public ResponseEntity<?> addProductToCart(@RequestBody Map<String, Object> request) {
        try {
            Long productId = Long.parseLong(request.get("productId").toString());
            int quantity = Integer.parseInt(request.get("quantity").toString());
            
            CartDetail cartDetail = cartService.addProductToCart(productId, quantity);
            return ResponseEntity.ok(cartDetail);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PreAuthorize("hasAnyRole('admin', 'employee', 'user')")
    @PutMapping("/update/{cartDetailId}")
    @ApiMessage("Cập nhật số lượng sản phẩm trong giỏ hàng")
    public ResponseEntity<?> updateCartItemQuantity(
            @PathVariable("cartDetailId") Long cartDetailId,
            @Valid @RequestBody UpdateCartItemDTO updateCartItemDTO) {
        try {
            CartDetail cartDetail = cartService.updateCartItemQuantity(cartDetailId, updateCartItemDTO.getQuantity());
            if (cartDetail == null) {
                return ResponseEntity.ok("Sản phẩm đã được xóa khỏi giỏ hàng do số lượng = 0");
            }
            return ResponseEntity.ok(cartDetail);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PreAuthorize("hasAnyRole('admin', 'employee', 'user')")
    @DeleteMapping("/remove/{cartDetailId}")
    @ApiMessage("Xóa sản phẩm khỏi giỏ hàng")
    public ResponseEntity<?> removeCartItem(@PathVariable("cartDetailId") Long cartDetailId) {
        try {
            cartService.removeCartItem(cartDetailId);
            return ResponseEntity.ok("Sản phẩm đã được xóa khỏi giỏ hàng");
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PreAuthorize("hasAnyRole('admin', 'employee', 'user')")
    @GetMapping
    @ApiMessage("Lấy danh sách sản phẩm trong giỏ hàng")
    public ResponseEntity<List<CartDetail>> getCartItems() {
        List<CartDetail> cartItems = cartService.getCartItems();
        return ResponseEntity.ok(cartItems);
    }

    @PreAuthorize("hasAnyRole('admin', 'employee', 'user')")
    @DeleteMapping("/clear")
    @ApiMessage("Xóa tất cả sản phẩm trong giỏ hàng")
    public ResponseEntity<?> clearCart() {
        cartService.clearCart();
        return ResponseEntity.ok("Giỏ hàng đã được làm trống");
    }
} 