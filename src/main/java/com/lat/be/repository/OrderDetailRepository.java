package com.lat.be.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.lat.be.domain.OrderDetail;
import com.lat.be.domain.Order;
import com.lat.be.domain.Product;

import java.util.List;

@Repository
public interface OrderDetailRepository extends JpaRepository<OrderDetail, Long>, JpaSpecificationExecutor<OrderDetail> {
    List<OrderDetail> findByOrder(Order order);
    List<OrderDetail> findByProduct(Product product);
} 