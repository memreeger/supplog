package com.supplog.repository;

import com.supplog.dto.user.ChangePasswordRequestDto;
import com.supplog.entity.User;
import com.supplog.enums.RoleName;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(attributePaths = "roles")
    List<User> findAll();

    Optional<User> findByUsername(String username); // AOP : declarative

    Optional<User> findByUsernameAndIsDeletedFalse(String username);

    Optional<User> findByEmail(String email);

    //Optional<User> findByNormalizedEmail(String email);  // FOR NORMALIZATION
    //Optional<User> findByNormalizedUsername(String username);

    @EntityGraph(attributePaths = "roles")
    List<User> findAllByIsDeletedFalse();

    Optional<User> findByIdAndIsDeletedFalse(Long id);

    @EntityGraph(attributePaths = "roles")
    List<User> findAllByIsDeletedTrue();

    boolean existsByIdAndIsDeletedFalse(Long id);

    @Query("""
        SELECT COUNT(DISTINCT u.id)
        FROM User u
        JOIN u.roles r
        WHERE r.name = :roleName
          AND u.isDeleted = false
        """)
    long countActiveUsersByRole(
            @Param("roleName") RoleName roleName
    );
    //void changePassword(String email, ChangePasswordRequestDto requestDto);
}
