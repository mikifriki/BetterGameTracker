package com.bettergametracker.security;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface ApplicationUserRepository extends JpaRepository<ApplicationUser, UUID> {
    Optional<ApplicationUser> findByGoogleSubject(String googleSubject);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO application_users (id, google_subject) "
            + "VALUES (:id, :subject) ON CONFLICT (google_subject) DO NOTHING", nativeQuery = true)
    void register(UUID id, String subject);
}

