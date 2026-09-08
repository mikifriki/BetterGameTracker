package com.bettergametracker.security;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "application_users")
public class ApplicationUser {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String googleSubject;

    protected ApplicationUser() {
        // Required by JPA.
    }

    public UUID getId() {
        return id;
    }
}
