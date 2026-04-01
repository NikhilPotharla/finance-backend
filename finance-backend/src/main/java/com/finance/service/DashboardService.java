package com.finance.service;

import com.finance.dto.CategoryTotalResponse;
import com.finance.dto.DashboardSummaryResponse;
import com.finance.dto.MonthlyTrendResponse;
import com.finance.dto.TransactionResponse;
import com.finance.model.Role;
import com.finance.model.TransactionType;
import com.finance.model.User;
import com.finance.repository.TransactionRepository;
import com.finance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final TransactionService transactionService;

    // ─────────────── Full Summary ───────────────

    public DashboardSummaryResponse getSummary(String email) {
        User user = getUser(email);
        boolean isAdmin = user.getRole() == Role.ADMIN;

        BigDecimal totalIncome = isAdmin
                ? transactionRepository.sumByType(TransactionType.CREDIT)
                : transactionRepository.sumByTypeAndOwner(TransactionType.CREDIT, user);

        BigDecimal totalExpenses = isAdmin
                ? transactionRepository.sumByType(TransactionType.DEBIT)
                : transactionRepository.sumByTypeAndOwner(TransactionType.DEBIT, user);

        totalIncome = totalIncome != null ? totalIncome : BigDecimal.ZERO;
        totalExpenses = totalExpenses != null ? totalExpenses : BigDecimal.ZERO;
        BigDecimal netBalance = totalIncome.subtract(totalExpenses);

        long totalTransactions = isAdmin
                ? transactionRepository.count()
                : transactionRepository.countByOwner(user);

        long totalCategories = isAdmin
                ? transactionRepository.countDistinctCategories()
                : transactionRepository.countDistinctCategoriesByOwner(user);

        List<CategoryTotalResponse> categories = getCategoryBreakdown(email);

        List<TransactionResponse> recent = (isAdmin
                ? transactionRepository.findTop10ByOrderByDateDesc()
                : transactionRepository.findTop10ByOwnerOrderByDateDesc(user))
                .stream()
                .map(transactionService::toResponse)
                .collect(Collectors.toList());

        return DashboardSummaryResponse.builder()
                .totalIncome(totalIncome)
                .totalExpenses(totalExpenses)
                .netBalance(netBalance)
                .totalTransactions(totalTransactions)
                .totalCategories(totalCategories)
                .categoryBreakdown(categories)
                .recentTransactions(recent)
                .build();
    }

    // ─────────────── Category Breakdown ───────────────

    public List<CategoryTotalResponse> getCategoryBreakdown(String email) {
        User user = getUser(email);
        boolean isAdmin = user.getRole() == Role.ADMIN;

        List<Object[]> raw = isAdmin
                ? transactionRepository.categoryTotals()
                : transactionRepository.categoryTotalsByOwner(user);

        // Compute grand total for percentage calculation
        BigDecimal grandTotal = raw.stream()
                .map(r -> (BigDecimal) r[1])
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return raw.stream().map(row -> {
            String category = (String) row[0];
            BigDecimal total = (BigDecimal) row[1];
            String pct = grandTotal.compareTo(BigDecimal.ZERO) > 0
                    ? total.divide(grandTotal, 4, RoundingMode.HALF_UP)
                           .multiply(BigDecimal.valueOf(100))
                           .setScale(1, RoundingMode.HALF_UP) + "%"
                    : "0%";
            return CategoryTotalResponse.builder()
                    .category(category)
                    .total(total)
                    .percentage(pct)
                    .build();
        }).collect(Collectors.toList());
    }

    // ─────────────── Monthly Trends (last 6 months) ───────────────

    public List<MonthlyTrendResponse> getMonthlyTrends(String email) {
        User user = getUser(email);
        boolean isAdmin = user.getRole() == Role.ADMIN;

        LocalDate from = LocalDate.now().minusMonths(5).withDayOfMonth(1);

        List<Object[]> raw = isAdmin
                ? transactionRepository.monthlyTrends(from)
                : transactionRepository.monthlyTrendsByUser(from, user.getId());

        // Group rows by year+month into a Map<"YYYY-MM", MonthlyTrendResponse>
        Map<String, MonthlyTrendResponse> trendMap = new LinkedHashMap<>();

        for (Object[] row : raw) {
            int year = ((Number) row[0]).intValue();
            int month = ((Number) row[1]).intValue();
            String type = (String) row[2];
            BigDecimal total = new BigDecimal(row[3].toString());

            String key = year + "-" + String.format("%02d", month);
            trendMap.putIfAbsent(key, MonthlyTrendResponse.builder()
                    .year(year).month(month)
                    .monthName(Month.of(month).name().substring(0, 3))
                    .income(BigDecimal.ZERO).expenses(BigDecimal.ZERO).net(BigDecimal.ZERO)
                    .build());

            MonthlyTrendResponse trend = trendMap.get(key);
            if ("CREDIT".equals(type)) {
                trend.setIncome(trend.getIncome().add(total));
            } else {
                trend.setExpenses(trend.getExpenses().add(total));
            }
            trend.setNet(trend.getIncome().subtract(trend.getExpenses()));
        }

        return new ArrayList<>(trendMap.values());
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }
}
