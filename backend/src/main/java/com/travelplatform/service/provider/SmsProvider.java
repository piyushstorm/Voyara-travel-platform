package com.travelplatform.service.provider;

public interface SmsProvider {
    void sendSms(String phoneNumber, String message);
}
