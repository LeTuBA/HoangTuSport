package com.lat.be.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.lat.be.domain.OrderDetail;
import com.lat.be.domain.Order;
import com.lat.be.domain.Product;

import java.time.Instant;
import java.util.List;

@Repository
public interface OrderDetailRepository extends JpaRepository<OrderDetail, Long>, JpaSpecificationExecutor<OrderDetail> {
    List<OrderDetail> findByOrder(Order order);
    List<OrderDetail> findByProduct(Product product);
    
    @Query("SELECT od.product.id as productId, od.product.name as productName, od.product.image as productImage, " +
           "SUM(od.quantity) as totalQuantitySold, SUM(od.totalPrice) as totalRevenue " +
           "FROM OrderDetail od " +
           "GROUP BY od.product.id, od.product.name, od.product.image " +
           "ORDER BY totalQuantitySold DESC")
    List<Object[]> findTopSellingProducts();
    
    @Query("SELECT od.product.id as productId, od.product.name as productName, od.product.image as productImage, " +
           "SUM(od.quantity) as totalQuantitySold, SUM(od.totalPrice) as totalRevenue " +
           "FROM OrderDetail od " +
           "WHERE od.order.createdAt >= :startDate AND od.order.createdAt <= :endDate " +
           "GROUP BY od.product.id, od.product.name, od.product.image " +
           "ORDER BY totalQuantitySold DESC")
    List<Object[]> findTopSellingProductsInPeriod(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);
    
    @Query("SELECT SUM(od.quantity) FROM OrderDetail od")
    Long countTotalProductsSold();
    
    @Query("SELECT SUM(od.quantity) FROM OrderDetail od WHERE od.order.createdAt >= :startDate AND od.order.createdAt <= :endDate")
    Long countProductsSoldInPeriod(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);
} 