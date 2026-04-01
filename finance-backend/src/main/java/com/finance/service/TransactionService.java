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
import com.finance.specification.TransactionSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    // ─────────────── Create ───────────────

    public TransactionResponse create(TransactionRequest request, String email) {
        User user = getUserByEmail(email);
        Transaction transaction = Transaction.builder()
                .title(request.getTitle())
                .amount(request.getAmount())
                .type(request.getType())
                .category(request.getCategory())
                .date(request.getDate())
                .description(request.getDescription())
                .owner(user)
                .build();
        return toResponse(transactionRepository.save(transaction));
    }

    // ─────────────── Read (paginated + filtered) ───────────────

    public Page<TransactionResponse> getAll(
            String email,
            String keyword,
            TransactionType type,
            String category,
            LocalDate dateFrom,
            LocalDate dateTo,
            Pageable pageable) {

        User user = getUserByEmail(email);
        boolean isAdmin = user.getRole() == Role.ADMIN;

        // Build specification dynamically
        Specification<Transaction> spec = Specification
                .where(isAdmin ? null : TransactionSpecification.hasOwner(user))
                .and(TransactionSpecification.hasType(type))
                .and(TransactionSpecification.hasCategory(category))
                .and(TransactionSpecification.dateAfter(dateFrom))
                .and(TransactionSpecification.dateBefore(dateTo))
                .and(TransactionSpecification.keywordSearch(keyword));

        return transactionRepository.findAll(spec, pageable).map(this::toResponse);
    }

    // ─────────────── Read Single ───────────────

    public TransactionResponse getById(Long id, String email) {
        User user = getUserByEmail(email);
        Transaction transaction = getTransactionById(id);
        checkOwnershipOrAdmin(transaction, user);
        return toResponse(transaction);
    }

    // ─────────────── Update ───────────────

    public TransactionResponse update(Long id, TransactionRequest request, String email) {
        User user = getUserByEmail(email);
        Transaction transaction = getTransactionById(id);
        checkOwnershipOrAdmin(transaction, user);

        transaction.setTitle(request.getTitle());
        transaction.setAmount(request.getAmount());
        transaction.setType(request.getType());
        transaction.setCategory(request.getCategory());
        transaction.setDate(request.getDate());
        transaction.setDescription(request.getDescription());

        return toResponse(transactionRepository.save(transaction));
    }

    // ─────────────── Delete ───────────────

    public void delete(Long id, String email) {
        User user = getUserByEmail(email);
        Transaction transaction = getTransactionById(id);
        checkOwnershipOrAdmin(transaction, user);
        transactionRepository.delete(transaction);
    }

    // ─────────────── Helpers ───────────────

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    private Transaction getTransactionById(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));
    }

    private void checkOwnershipOrAdmin(Transaction transaction, User user) {
        if (user.getRole() != Role.ADMIN && !transaction.getOwner().getId().equals(user.getId())) {
            throw new AccessDeniedException("You do not have permission to access this transaction");
        }
    }

    public TransactionResponse toResponse(Transaction t) {
        return TransactionResponse.builder()
                .id(t.getId())
                .title(t.getTitle())
                .amount(t.getAmount())
                .type(t.getType())
                .category(t.getCategory())
                .date(t.getDate())
                .description(t.getDescription())
                .ownerEmail(t.getOwner().getEmail())
                .ownerName(t.getOwner().getName())
                .build();
    }
}
