package com.project.BharatConnect.repo;

import com.project.BharatConnect.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.UUID;

@EnableJpaRepositories
public interface UserRepository extends JpaRepository<User, UUID> {

    User findByEmail(String email);

}
