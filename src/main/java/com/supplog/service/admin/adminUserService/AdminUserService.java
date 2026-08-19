package com.supplog.service.admin.adminUserService;


import com.supplog.dto.admin.user.ResetPasswordRequestDto;
import com.supplog.dto.admin.user.UpdateUserProfileRequestDtoByAdmin;
import com.supplog.dto.admin.user.UpdateUserRoleRequestDto;
import com.supplog.dto.user.CreateUserRequestDto;
import com.supplog.dto.user.UpdateUserProfileRequestDto;
import com.supplog.dto.admin.user.AdminUserResponseDto;

import java.util.List;

public interface AdminUserService {
    AdminUserResponseDto getById(Long id);

    AdminUserResponseDto getByUserName(String username);

    AdminUserResponseDto getByEmail(String email);

    List<AdminUserResponseDto> getAll();

    List<AdminUserResponseDto> getAllActiveUsers();

    List<AdminUserResponseDto> getAllInactiveUsers();

    void addUser(CreateUserRequestDto userRequestDto);

    void deactivateUser(Long CurrentAdminId, Long userId);

    void activateUser(Long userId);

    void updateUserProfileByAdmin(Long id, UpdateUserProfileRequestDtoByAdmin userProfileRequestDto);

    void resetPassword(Long id, ResetPasswordRequestDto resetPasswordRequestDto);

    void updateRole(Long currentAdminId, Long id, UpdateUserRoleRequestDto updateUserRoleRequestDto);
}
