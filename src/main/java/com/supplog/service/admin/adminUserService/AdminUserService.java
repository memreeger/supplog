package com.supplog.service.admin.adminUserService;


import com.supplog.dto.admin.user.ResetPasswordRequestDto;
import com.supplog.dto.user.CreateUserRequestDto;
import com.supplog.dto.user.UpdateUserProfileRequestDto;
import com.supplog.dto.user.UserResponseDto;

import java.util.List;

public interface AdminUserService {
    UserResponseDto getById(Long id);

    UserResponseDto getByUserName(String username);

    UserResponseDto getByEmail(String email);

    List<UserResponseDto> getAll();

    List<UserResponseDto> getAllActiveUsers();

    List<UserResponseDto> getAllInactiveUsers();

    void addUser(CreateUserRequestDto userRequestDto);

    void deactivateUser(Long userId);

    void activateUser(Long userId);

    void updateUserProfileByAdmin(Long id, UpdateUserProfileRequestDto userProfileRequestDto);

    void resetPassword(Long id, ResetPasswordRequestDto resetPasswordRequestDto);
}
