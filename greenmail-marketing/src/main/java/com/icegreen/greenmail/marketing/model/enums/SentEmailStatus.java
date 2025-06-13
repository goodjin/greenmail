package com.icegreen.greenmail.marketing.model.enums;

public enum SentEmailStatus {
    PREPARING,
    SENT,
    FAILED,
    BOUNCED,
    OPENED,
    CLICKED
    // Note: OPENED and CLICKED typically require tracking pixel/link integration
}
