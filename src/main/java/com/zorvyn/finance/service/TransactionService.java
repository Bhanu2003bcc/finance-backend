package com.zorvyn.finance.service;

import com.zorvyn.finance.dto.request.TransactionFilterRequest;
import com.zorvyn.finance.dto.request.TransactionRequest;
import com.zorvyn.finance.dto.response.TransactionResponse;
import com.zorvyn.finance.model.User;
import org.springframework.data.domain.Page;

public interface TransactionService {

    TransactionResponse createTransaction(TransactionRequest request, User currentUser);

    Page<TransactionResponse> getTransactions(TransactionFilterRequest filter);

    TransactionResponse getTransactionById(Long id);

    TransactionResponse updateTransaction(Long id, TransactionRequest request, User currentUser);

    void deleteTransaction(Long id, User currentUser);

    /**
     * Exports filtered transactions to a CSV byte array.
     */
    byte[] exportTransactionsToCsv(com.zorvyn.finance.dto.request.TransactionFilterRequest filter);
}
