package com.lat.be.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.lat.be.domain.ImportDetail;
import com.lat.be.domain.ImportHistory;
import com.lat.be.domain.Product;

import java.util.List;

@Repository
public interface ImportDetailRepository extends JpaRepository<ImportDetail, Long>, JpaSpecificationExecutor<ImportDetail> {
    List<ImportDetail> findByImportHistory(ImportHistory importHistory);
    List<ImportDetail> findByProduct(Product product);
} 