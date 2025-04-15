package com.lat.be.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lat.be.domain.Cart;
import com.lat.be.domain.CartDetail;
import com.lat.be.domain.Product;

import java.util.List;

@Repository
public interface CartDetailRepository extends JpaRepository<CartDetail, Long> {
    CartDetail findByCartAndProduct(Cart cart, Product product);
    List<CartDetail> findByCart(Cart cart);
    void deleteByCart(Cart cart);
} 