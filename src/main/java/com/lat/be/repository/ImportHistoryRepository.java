package com.lat.be.repository;

import com.lat.be.domain.ImportHistory;
import com.lat.be.domain.Supplier;
import com.lat.be.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImportHistoryRepository extends JpaRepository<ImportHistory, Long>, JpaSpecificationExecutor<ImportHistory> {
    List<ImportHistory> findByUser(User user);
    List<ImportHistory> findBySupplier(Supplier supplier);
} 