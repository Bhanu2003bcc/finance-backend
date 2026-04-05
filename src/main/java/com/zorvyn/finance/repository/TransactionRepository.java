package com.zorvyn.finance.repository;

import com.zorvyn.finance.model.Transaction;
import com.zorvyn.finance.model.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long>,
        JpaSpecificationExecutor<Transaction> {

    // --------------------------------------------------------
    // Basic finders (soft-delete aware)
    // --------------------------------------------------------

    Optional<Transaction> findByIdAndDeletedFalse(Long id);

    Page<Transaction> findAllByDeletedFalse(Pageable pageable);

    // --------------------------------------------------------
    // Dashboard aggregations
    // --------------------------------------------------------

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.type = :type AND t.deleted = false")
    BigDecimal sumByType(@Param("type") TransactionType type);

    @Query("SELECT t.category, COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.deleted = false GROUP BY t.category ORDER BY SUM(t.amount) DESC")
    List<Object[]> sumGroupedByCategory();

    @Query("SELECT t FROM Transaction t WHERE t.deleted = false " +
           "ORDER BY t.date DESC, t.createdAt DESC")
    List<Transaction> findRecentTransactions(Pageable pageable);

    // --------------------------------------------------------
    // Monthly trend aggregation
    // --------------------------------------------------------

    @Query(value = """
        SELECT
            TO_CHAR(t.date, 'YYYY-MM')   AS month,
            t.type                        AS type,
            COALESCE(SUM(t.amount), 0)   AS total
        FROM transactions t
        WHERE t.deleted = false
          AND t.date >= :startDate
        GROUP BY TO_CHAR(t.date, 'YYYY-MM'), t.type
        ORDER BY month ASC
        """, nativeQuery = true)
    List<Object[]> findMonthlyTrends(@Param("startDate") LocalDate startDate);

    // --------------------------------------------------------
    // Category-wise filtered totals
    // --------------------------------------------------------

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.type = :type AND t.category = :category AND t.deleted = false")
    BigDecimal sumByTypeAndCategory(@Param("type") TransactionType type,
                                    @Param("category") String category);

    // --------------------------------------------------------
    // Existence check for user-owned transaction (used in delete/update)
    // --------------------------------------------------------

    boolean existsByIdAndDeletedFalse(Long id);
}
