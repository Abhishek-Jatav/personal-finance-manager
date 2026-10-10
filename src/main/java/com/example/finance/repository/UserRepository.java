package com.example.finance.repository;

import com.example.finance.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Database access for users. */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
