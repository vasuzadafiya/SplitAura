package com.example.splitaura.models;

public class UpiResponse {
    private String status;
    private String transactionId;
    private String responseCode;
    private String upiApp;

    public UpiResponse(String status, String transactionId, String responseCode, String upiApp) {
        this.status = status;
        this.transactionId = transactionId != null ? transactionId : "N/A";
        this.responseCode = responseCode != null ? responseCode : "N/A";
        this.upiApp = upiApp;
    }

    public String getStatus() {
        return status;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getResponseCode() {
        return responseCode;
    }

    public String getUpiApp() {
        return upiApp;
    }
}