package com.oop.disaster.alert.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * A person who receives alerts. {@code hazards} is a comma-separated list
 * (e.g. "FLOOD,FIRE") or "ALL".
 */
@Entity
@Table(name = "alert_subscriber")
public class Subscriber {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String name;

    @Email
    private String email;

    /** WhatsApp number in international format, e.g. +263771234567. */
    @Pattern(regexp = "^$|^\\+?[0-9]{9,15}$", message = "Phone must be in international format")
    private String phone;

    @NotBlank
    private String hazards = "ALL";

    private boolean active = true;

    public Subscriber() {
    }

    public Subscriber(String name, String email, String phone, String hazards) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.hazards = hazards;
    }

    /** True if this subscriber wants alerts for the given hazard. */
    public boolean wants(String hazard) {
        if (!active || hazards == null) {
            return false;
        }
        for (String h : hazards.split(",")) {
            String t = h.trim();
            if (t.equalsIgnoreCase("ALL") || t.equalsIgnoreCase(hazard)) {
                return true;
            }
        }
        return false;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getHazards() { return hazards; }
    public void setHazards(String hazards) { this.hazards = hazards; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
