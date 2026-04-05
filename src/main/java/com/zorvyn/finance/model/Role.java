package com.zorvyn.finance.model;

/**
 * Defines the access roles available in the system.
 *
 * VIEWER  — read-only access to dashboard data
 * ANALYST — read records + access analytics/summaries
 * ADMIN   — full CRUD on records and user management
 */
public enum Role {
    VIEWER,                   // Can only view dashboard data
    ANALYST,                  // Can view records and access insights
    ADMIN                     // Can create, update, and manage records and users
}
