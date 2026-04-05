package com.zorvyn.finance.service.impl;

import com.opencsv.CSVWriter;
import com.zorvyn.finance.dto.request.TransactionFilterRequest;
import com.zorvyn.finance.dto.request.TransactionRequest;
import com.zorvyn.finance.dto.response.TransactionResponse;
import com.zorvyn.finance.exception.AppExceptions;
import com.zorvyn.finance.model.Role;
import com.zorvyn.finance.model.Transaction;
import com.zorvyn.finance.model.User;
import com.zorvyn.finance.repository.TransactionRepository;
import com.zorvyn.finance.repository.TransactionSpecification;
import com.zorvyn.finance.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;

    @Override
    @Transactional
    public TransactionResponse createTransaction(TransactionRequest request, User currentUser) {
        Transaction transaction = Transaction.builder()
                .amount(request.getAmount())
                .type(request.getType())
                .category(request.getCategory())
                .date(request.getDate())
                .notes(request.getNotes())
                .createdBy(currentUser)
                .build();

        transaction = transactionRepository.save(transaction);
        log.info("Transaction created: id={} by user={}", transaction.getId(), currentUser.getEmail());
        return TransactionResponse.from(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactions(TransactionFilterRequest filter) {
        // Clamp page size between 1 and 100
        int size = Math.max(1, Math.min(filter.getSize(), 100));
        int page = Math.max(0, filter.getPage());

        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "date", "createdAt"));

        return transactionRepository
                .findAll(TransactionSpecification.withFilters(filter), pageable)
                .map(TransactionResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionResponse getTransactionById(Long id) {
        Transaction transaction = findActiveTransactionOrThrow(id);
        return TransactionResponse.from(transaction);
    }

    @Override
    @Transactional
    public TransactionResponse updateTransaction(Long id, TransactionRequest request,
                                                  User currentUser) {
        Transaction transaction = findActiveTransactionOrThrow(id);
        assertWritePermission(transaction, currentUser, "update");

        transaction.setAmount(request.getAmount());
        transaction.setType(request.getType());
        transaction.setCategory(request.getCategory());
        transaction.setDate(request.getDate());
        transaction.setNotes(request.getNotes());

        transaction = transactionRepository.save(transaction);
        log.info("Transaction {} updated by {}", id, currentUser.getEmail());
        return TransactionResponse.from(transaction);
    }

    @Override
    @Transactional
    public void deleteTransaction(Long id, User currentUser) {
        Transaction transaction = findActiveTransactionOrThrow(id);
        assertWritePermission(transaction, currentUser, "delete");

        // Soft delete — record stays in DB for audit
        transaction.setDeleted(true);
        transactionRepository.save(transaction);
        log.info("Transaction {} soft-deleted by {}", id, currentUser.getEmail());
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportTransactionsToCsv(TransactionFilterRequest filter) {
        // Fetch ALL matching transactions (bypassing pagination for export)
        List<Transaction> transactions = transactionRepository.findAll(
                TransactionSpecification.withFilters(filter),
                Sort.by(Sort.Direction.DESC, "date", "createdAt")
        );

        StringWriter stringWriter = new StringWriter();

        try (CSVWriter csvWriter = new CSVWriter(stringWriter)) {
            // 1. Write the CSV Header Row
            String[] header = { "Transaction ID", "Type", "Amount", "Category", "Date", "Notes", "Created By", "Created At" };
            csvWriter.writeNext(header);

            // 2. Write the Data Rows
            for (Transaction t : transactions) {
                String[] data = {
                        String.valueOf(t.getId()),
                        t.getType().name(),
                        String.valueOf(t.getAmount()),
                        t.getCategory(),
                        t.getDate().toString(),
                        t.getNotes() != null ? t.getNotes() : "",
                        t.getCreatedBy().getEmail(),
                        t.getCreatedAt().toString()
                };
                csvWriter.writeNext(data);
            }
        } catch (Exception e) {
            log.error("Failed to generate CSV export", e);
            throw new RuntimeException("Error generating CSV file");
        }

        // Return as a UTF-8 byte array ready for HTTP transmission
        return stringWriter.toString().getBytes(StandardCharsets.UTF_8);
    }

    // Helpers

    private Transaction findActiveTransactionOrThrow(Long id) {
        return transactionRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new AppExceptions.ResourceNotFoundException(
                        "Transaction not found with id: " + id));
    }

    /**
     * ANALYST can only modify their own transactions.
     * ADMIN can modify any transaction.
     */
    private void assertWritePermission(Transaction transaction, User currentUser, String action) {
        if (currentUser.getRole() == Role.ADMIN) return;

        if (!transaction.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new AppExceptions.AccessDeniedException(
                    "You are not allowed to " + action + " a transaction created by another user.");
        }
    }
}
