package com.finance.model;

public enum Role {
    VIEWER,   // Can only view dashboard summary data
    ANALYST,  // Can view records and access insights/analytics
    ADMIN     // Full access: manage records, users, and all data
}
