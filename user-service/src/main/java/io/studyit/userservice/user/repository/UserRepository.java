package io.studyit.userservice.user.repository;

import io.studyit.userservice.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByUserId(String userId);

    boolean existsByUserId(String userId);
}
