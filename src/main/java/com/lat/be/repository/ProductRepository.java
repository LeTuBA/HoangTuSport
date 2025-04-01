package com.lat.be.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.lat.be.domain.Product;
import com.lat.be.domain.Category;
import com.lat.be.domain.Supplier;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    List<Product> findByCategory(Category category);
    List<Product> findBySupplier(Supplier supplier);
    Product findByName(String name);
    boolean existsByName(String name);
    Page<Product> findAll(Pageable pageable);
} 