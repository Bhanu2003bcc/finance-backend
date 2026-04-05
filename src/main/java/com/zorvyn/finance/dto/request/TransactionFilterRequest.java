package com.zorvyn.finance.dto.request;

import com.zorvyn.finance.model.TransactionType;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Query parameters for filtering/searching transactions.
 * All fields are optional — omitting a field means "no filter on that field".
 */
@Data
public class TransactionFilterRequest {

    private TransactionType type;
    private String category;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    /** Keyword search in notes or category */
    private String keyword;

    // Pagination
    private int page = 0;
    private int size = 20;
}
