package com.finance.repository;

import com.finance.model.Transaction;
import com.finance.model.TransactionType;
import com.finance.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long>,
        JpaSpecificationExecutor<Transaction> {

    // ──────────── Dashboard: Sum by type ────────────

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.type = :type AND t.owner = :owner")
    BigDecimal sumByTypeAndOwner(@Param("type") TransactionType type, @Param("owner") User owner);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.type = :type")
    BigDecimal sumByType(@Param("type") TransactionType type);

    // ──────────── Dashboard: Category totals ────────────

    @Query("SELECT t.category, SUM(t.amount) FROM Transaction t WHERE t.owner = :owner GROUP BY t.category ORDER BY SUM(t.amount) DESC")
    List<Object[]> categoryTotalsByOwner(@Param("owner") User owner);

    @Query("SELECT t.category, SUM(t.amount) FROM Transaction t GROUP BY t.category ORDER BY SUM(t.amount) DESC")
    List<Object[]> categoryTotals();

    // ──────────── Dashboard: Recent activity ────────────

    List<Transaction> findTop10ByOwnerOrderByDateDesc(User owner);

    List<Transaction> findTop10ByOrderByDateDesc();

    // ──────────── Dashboard: Monthly trends (native SQL for MySQL compatibility) ────────────

    @Query(value = "SELECT YEAR(date) AS year, MONTH(date) AS month, type, SUM(amount) AS total " +
                   "FROM transactions WHERE date >= :from AND user_id = :userId " +
                   "GROUP BY YEAR(date), MONTH(date), type ORDER BY year ASC, month ASC",
           nativeQuery = true)
    List<Object[]> monthlyTrendsByUser(@Param("from") LocalDate from, @Param("userId") Long userId);

    @Query(value = "SELECT YEAR(date) AS year, MONTH(date) AS month, type, SUM(amount) AS total " +
                   "FROM transactions WHERE date >= :from " +
                   "GROUP BY YEAR(date), MONTH(date), type ORDER BY year ASC, month ASC",
           nativeQuery = true)
    List<Object[]> monthlyTrends(@Param("from") LocalDate from);

    // ──────────── Dashboard: Count stats ────────────

    long countByOwner(User owner);

    @Query("SELECT COUNT(DISTINCT t.category) FROM Transaction t WHERE t.owner = :owner")
    long countDistinctCategoriesByOwner(@Param("owner") User owner);

    @Query("SELECT COUNT(DISTINCT t.category) FROM Transaction t")
    long countDistinctCategories();
}
