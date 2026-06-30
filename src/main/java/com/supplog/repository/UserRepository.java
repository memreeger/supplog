package com.supplog.repository;

import com.supplog.dto.user.ChangePasswordRequestDto;
import com.supplog.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username); // AOP : declarative

    Optional<User> findByEmail(String email);

    List<User> findAllByIsDeletedFalse();

    List<User> findAllByIsDeletedTrue();

    Optional<User> findByUsernameAndIsDeletedFalse(String username);

    //void changePassword(String email, ChangePasswordRequestDto requestDto);
}
