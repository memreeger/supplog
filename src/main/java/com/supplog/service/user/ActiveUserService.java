package com.supplog.service.user;

import com.supplog.entity.User;
import com.supplog.exception.ResourceNotFoundException;
import com.supplog.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ActiveUserService {

    private final UserRepository userRepository;

    public ActiveUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getRequiredActiveUser(Long userId) {
        return userRepository
                .findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "user.not.found",
                                userId
                        )
                );
    }

    public void requireActiveUser(Long userId) {
        getRequiredActiveUser(userId);
    }
}
