package com.zorvyn.finance.repository;

import com.zorvyn.finance.model.Role;
import com.zorvyn.finance.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findAllByOrderByCreatedAtDesc();

    List<User> findByRole(Role role);

    List<User> findByActive(boolean active);
}
