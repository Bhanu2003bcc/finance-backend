package com.zorvyn.finance.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents a financial record (income or expense entry).
 * Soft-delete is supported via the `deleted` flag.
 */
@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_tx_user_id",   columnList = "user_id"),
    @Index(name = "idx_tx_type",      columnList = "type"),
    @Index(name = "idx_tx_category",  columnList = "category"),
    @Index(name = "idx_tx_date",      columnList = "date"),
    @Index(name = "idx_tx_deleted",   columnList = "deleted")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The user who created / owns this transaction.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User createdBy;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionType type;

    /**
     * Free-text category (e.g. "Salary", "Rent", "Food").
     * Stored as lowercase for consistent aggregations.
     */
    @Column(nullable = false, length = 100)
    private String category;

    @Column(nullable = false)
    private LocalDate date;

    @Column(length = 500)
    private String notes;

    /**
     * Soft delete flag — deleted records are excluded from
     * normal queries but kept in the database for audit trail.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean deleted = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (category != null) category = category.trim().toLowerCase();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        if (category != null) category = category.trim().toLowerCase();
    }
}
