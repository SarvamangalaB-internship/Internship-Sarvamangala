package com.urbanvogue.notification.dto;

import lombok.Data;

@Data
public class NotificationRequest {

    private String recipientUsername;
    private String type;
    private String message;
    private String channel;  // "EMAIL" or "SMS"
    private Long referenceId; // orderId or paymentId

}