package com.example.splitaura.fragments;

public interface PaymentResultCallback {
    void onPaymentSuccess(String paymentId);
    void onPaymentError(int code, String response);
}