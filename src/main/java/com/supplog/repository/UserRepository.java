package com.supplog.repository;

import com.supplog.dto.user.ChangePasswordRequestDto;
import com.supplog.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface UserRepository extends JpaRepository<User, Long> {


    Optional<User> findByUserName(String userName); // AOP : declarative

    Optional<User> findByEmail(String email);

    //void changePassword(String email, ChangePasswordRequestDto requestDto);
}
