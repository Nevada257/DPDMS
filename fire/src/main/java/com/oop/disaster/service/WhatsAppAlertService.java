package com.oop.disaster.service;

import org.springframework.stereotype.Service;

@Service
public class WhatsAppAlertService {

    public void send(String phoneNumber, String message) {
        System.out.println("WHATSAPP DEMO: " + phoneNumber + " -> " + message);
    }
}
