package com.zorvyn.finance;

import com.zorvyn.finance.dto.request.TransactionRequest;
import com.zorvyn.finance.dto.response.TransactionResponse;
import com.zorvyn.finance.exception.AppExceptions;
import com.zorvyn.finance.model.Role;
import com.zorvyn.finance.model.Transaction;
import com.zorvyn.finance.model.TransactionType;
import com.zorvyn.finance.model.User;
import com.zorvyn.finance.repository.TransactionRepository;
import com.zorvyn.finance.service.impl.TransactionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionService Unit Tests")
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private User adminUser;
    private User analystUser;
    private User otherAnalyst;
    private Transaction existingTx;
    private TransactionRequest txRequest;

    @BeforeEach
    void setUp() {
        adminUser = User.builder()
                .id(1L).fullName("Admin").email("admin@test.com")
                .role(Role.ADMIN).active(true).build();

        analystUser = User.builder()
                .id(2L).fullName("Analyst").email("analyst@test.com")
                .role(Role.ANALYST).active(true).build();

        otherAnalyst = User.builder()
                .id(3L).fullName("Other").email("other@test.com")
                .role(Role.ANALYST).active(true).build();

        existingTx = Transaction.builder()
                .id(10L)
                .amount(new BigDecimal("500.00"))
                .type(TransactionType.INCOME)
                .category("salary")
                .date(LocalDate.now())
                .createdBy(analystUser)
                .deleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        txRequest = new TransactionRequest();
        txRequest.setAmount(new BigDecimal("250.00"));
        txRequest.setType(TransactionType.EXPENSE);
        txRequest.setCategory("Food");
        txRequest.setDate(LocalDate.now());
        txRequest.setNotes("Lunch");
    }

    // -------------------------------------------------------
    // Create
    // -------------------------------------------------------

    @Test
    @DisplayName("createTransaction() — persists and returns response")
    void create_success() {
        Transaction saved = Transaction.builder()
                .id(1L).amount(txRequest.getAmount())
                .type(txRequest.getType()).category("food")
                .date(txRequest.getDate()).notes(txRequest.getNotes())
                .createdBy(analystUser).deleted(false)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        when(transactionRepository.save(any())).thenReturn(saved);

        TransactionResponse response = transactionService.createTransaction(txRequest, analystUser);

        assertThat(response).isNotNull();
        assertThat(response.getAmount()).isEqualTo(new BigDecimal("250.00"));
        verify(transactionRepository).save(any(Transaction.class));
    }

    // -------------------------------------------------------
    // Read
    // -------------------------------------------------------

    @Test
    @DisplayName("getTransactionById() — existing id returns response")
    void getById_found() {
        when(transactionRepository.findByIdAndDeletedFalse(10L))
                .thenReturn(Optional.of(existingTx));

        TransactionResponse response = transactionService.getTransactionById(10L);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getCategory()).isEqualTo("salary");
    }

    @Test
    @DisplayName("getTransactionById() — missing id throws ResourceNotFoundException")
    void getById_notFound_throws() {
        when(transactionRepository.findByIdAndDeletedFalse(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.getTransactionById(99L))
                .isInstanceOf(AppExceptions.ResourceNotFoundException.class);
    }

    // -------------------------------------------------------
    // Update
    // -------------------------------------------------------

    @Test
    @DisplayName("updateTransaction() — ADMIN can update any transaction")
    void update_admin_canUpdateAny() {
        when(transactionRepository.findByIdAndDeletedFalse(10L))
                .thenReturn(Optional.of(existingTx));
        when(transactionRepository.save(any())).thenReturn(existingTx);

        TransactionResponse response =
                transactionService.updateTransaction(10L, txRequest, adminUser);

        assertThat(response).isNotNull();
        verify(transactionRepository).save(any());
    }

    @Test
    @DisplayName("updateTransaction() — ANALYST cannot update another user's transaction")
    void update_analyst_otherOwner_throws() {
        when(transactionRepository.findByIdAndDeletedFalse(10L))
                .thenReturn(Optional.of(existingTx)); // owned by analystUser

        assertThatThrownBy(() ->
                transactionService.updateTransaction(10L, txRequest, otherAnalyst))
                .isInstanceOf(AppExceptions.AccessDeniedException.class);

        verify(transactionRepository, never()).save(any());
    }

    // -------------------------------------------------------
    // Delete
    // -------------------------------------------------------

    @Test
    @DisplayName("deleteTransaction() — soft-deletes record")
    void delete_success() {
        when(transactionRepository.findByIdAndDeletedFalse(10L))
                .thenReturn(Optional.of(existingTx));
        when(transactionRepository.save(any())).thenReturn(existingTx);

        transactionService.deleteTransaction(10L, analystUser);

        assertThat(existingTx.isDeleted()).isTrue();
        verify(transactionRepository).save(existingTx);
    }

    @Test
    @DisplayName("deleteTransaction() — ANALYST cannot delete another user's transaction")
    void delete_analyst_otherOwner_throws() {
        when(transactionRepository.findByIdAndDeletedFalse(10L))
                .thenReturn(Optional.of(existingTx));

        assertThatThrownBy(() ->
                transactionService.deleteTransaction(10L, otherAnalyst))
                .isInstanceOf(AppExceptions.AccessDeniedException.class);
    }
}
