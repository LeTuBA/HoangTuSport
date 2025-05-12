package com.lat.be.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.lat.be.domain.Order;
import com.lat.be.domain.User;
import com.lat.be.util.constant.PaymentMethod;
import com.lat.be.util.constant.PaymentStatus;

import java.time.Instant;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {
    List<Order> findByUser(User user);
    List<Order> findByPaymentMethod(PaymentMethod paymentMethod);
    
    @Query("SELECT COUNT(o) FROM Order o WHERE o.createdAt >= :startDate AND o.createdAt <= :endDate")
    Long countOrdersInPeriod(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);
    
    @Query("SELECT COUNT(o) FROM Order o")
    Long countTotalOrders();
    
    @Query("SELECT SUM(o.totalPrice) FROM Order o WHERE o.paymentStatus = :paymentStatus AND o.createdAt >= :startDate AND o.createdAt <= :endDate")
    Long sumRevenueInPeriod(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate, @Param("paymentStatus") PaymentStatus paymentStatus);
    
    @Query("SELECT SUM(o.totalPrice) FROM Order o WHERE o.paymentStatus = :paymentStatus")
    Long sumTotalRevenue(@Param("paymentStatus") PaymentStatus paymentStatus);
    
    @Query("SELECT COUNT(DISTINCT o.user.id) FROM Order o WHERE o.createdAt >= :startDate AND o.createdAt <= :endDate")
    Long countCustomersInPeriod(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);
    
    @Query("SELECT COUNT(DISTINCT o.user.id) FROM Order o")
    Long countTotalCustomers();
} 