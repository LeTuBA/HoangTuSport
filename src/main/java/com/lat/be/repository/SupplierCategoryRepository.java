package com.lat.be.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.lat.be.domain.SupplierCategory;
import com.lat.be.domain.Supplier;
import com.lat.be.domain.Category;

import java.util.List;

@Repository
public interface SupplierCategoryRepository extends JpaRepository<SupplierCategory, Long>, JpaSpecificationExecutor<SupplierCategory> {
    List<SupplierCategory> findBySupplier(Supplier supplier);
    List<SupplierCategory> findByCategory(Category category);
    SupplierCategory findBySupplierAndCategory(Supplier supplier, Category category);
    
    @Modifying
    @Query("DELETE FROM SupplierCategory sc WHERE sc.supplier.id = :supplierId")
    void deleteAllBySupplierIdNative(@Param("supplierId") Long supplierId);
} 