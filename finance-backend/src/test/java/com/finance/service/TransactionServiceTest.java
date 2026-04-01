package com.finance.service;

import com.finance.dto.TransactionRequest;
import com.finance.dto.TransactionResponse;
import com.finance.exception.ResourceNotFoundException;
import com.finance.model.Role;
import com.finance.model.Transaction;
import com.finance.model.TransactionType;
import com.finance.model.User;
import com.finance.repository.TransactionRepository;
import com.finance.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private TransactionService transactionService;

    private User regularUser;
    private User adminUser;
    private Transaction transaction;
    private TransactionRequest request;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        regularUser = User.builder().id(1L).name("John").email("john@example.com")
                .password("enc").role(Role.ANALYST).active(true).build();

        adminUser = User.builder().id(2L).name("Admin").email("admin@example.com")
                .password("enc").role(Role.ADMIN).active(true).build();

        request = new TransactionRequest();
        request.setTitle("Rent");
        request.setAmount(BigDecimal.valueOf(1500));
        request.setType(TransactionType.DEBIT);
        request.setCategory("Housing");
        request.setDate(LocalDate.now());
        request.setDescription("Monthly rent");

        transaction = Transaction.builder()
                .id(1L)
                .title("Rent")
                .amount(BigDecimal.valueOf(1500))
                .type(TransactionType.DEBIT)
                .category("Housing")
                .date(LocalDate.now())
                .description("Monthly rent")
                .owner(regularUser)
                .build();

        pageable = PageRequest.of(0, 10);
    }

    // ─────────────── Create ───────────────

    @Test
    @DisplayName("Create: admin can create transaction")
    void create_success() {
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(adminUser));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            return Transaction.builder().id(1L).title(t.getTitle()).amount(t.getAmount())
                    .type(t.getType()).category(t.getCategory()).date(t.getDate())
                    .description(t.getDescription()).owner(adminUser).build();
        });

        TransactionResponse response = transactionService.create(request, "admin@example.com");

        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Rent");
        assertThat(response.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(1500));
        verify(transactionRepository).save(any(Transaction.class));
    }

    // ─────────────── Get By ID ───────────────

    @Test
    @DisplayName("GetById: analyst can access their own transaction")
    void getById_userOwnsTransaction() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(regularUser));
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction));

        TransactionResponse response = transactionService.getById(1L, "john@example.com");

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getOwnerEmail()).isEqualTo("john@example.com");
    }

    @Test
    @DisplayName("GetById: admin can access any transaction")
    void getById_adminCanAccessAll() {
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(adminUser));
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction));

        TransactionResponse response = transactionService.getById(1L, "admin@example.com");
        assertThat(response.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("GetById: user cannot access another user's transaction")
    void getById_userAccessDenied() {
        User otherUser = User.builder().id(3L).email("other@example.com").role(Role.ANALYST)
                .name("Other").active(true).build();
        when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(otherUser));
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction));

        assertThatThrownBy(() -> transactionService.getById(1L, "other@example.com"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("GetById: throws 404 when transaction not found")
    void getById_notFound() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(regularUser));
        when(transactionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.getById(99L, "john@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─────────────── Get All (with Specification) ───────────────

    @Test
    @DisplayName("GetAll: analyst only sees their own transactions (via Specification)")
    void getAll_analystScopedTransactions() {
        Page<Transaction> page = new PageImpl<>(List.of(transaction));
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(regularUser));
        // Specification-based findAll
        when(transactionRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        Page<TransactionResponse> result = transactionService.getAll(
                "john@example.com", null, null, null, null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getOwnerEmail()).isEqualTo("john@example.com");
    }

    @Test
    @DisplayName("GetAll: admin sees all (Specification with no owner filter)")
    void getAll_adminSeesAll() {
        Transaction adminTx = Transaction.builder().id(2L).title("Salary").amount(BigDecimal.valueOf(5000))
                .type(TransactionType.CREDIT).category("Income").date(LocalDate.now()).owner(adminUser).build();
        Page<Transaction> page = new PageImpl<>(List.of(transaction, adminTx));

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(adminUser));
        when(transactionRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        Page<TransactionResponse> result = transactionService.getAll(
                "admin@example.com", null, null, null, null, null, pageable);

        assertThat(result.getContent()).hasSize(2);
    }

    // ─────────────── Delete ───────────────

    @Test
    @DisplayName("Delete: admin can delete any transaction")
    void delete_adminCanDelete() {
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(adminUser));
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction));

        assertThatCode(() -> transactionService.delete(1L, "admin@example.com"))
                .doesNotThrowAnyException();

        verify(transactionRepository).delete(transaction);
    }

    @Test
    @DisplayName("Delete: user cannot delete another user's transaction")
    void delete_userAccessDenied() {
        User otherUser = User.builder().id(3L).email("other@example.com").role(Role.ANALYST)
                .name("Other").active(true).build();
        when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(otherUser));
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction));

        assertThatThrownBy(() -> transactionService.delete(1L, "other@example.com"))
                .isInstanceOf(AccessDeniedException.class);

        verify(transactionRepository, never()).delete(any(Transaction.class));
    }
}
