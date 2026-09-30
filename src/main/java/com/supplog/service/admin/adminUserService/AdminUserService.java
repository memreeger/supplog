package com.supplog.service.admin.adminUserService;


import com.supplog.dto.admin.user.ResetPasswordRequestDto;
import com.supplog.dto.admin.user.UpdateUserProfileRequestDtoByAdmin;
import com.supplog.dto.admin.user.UpdateUserRoleRequestDto;
import com.supplog.dto.user.CreateUserRequestDto;
import com.supplog.dto.user.UpdateUserProfileRequestDto;
import com.supplog.dto.admin.user.AdminUserResponseDto;
import com.supplog.dto.admin.user.AdminUserDetailResponseDto;
import com.supplog.enums.RoleName;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminUserService {
    AdminUserResponseDto getById(Long id);

    AdminUserDetailResponseDto getDetail(Long id);

    AdminUserResponseDto getByUserName(String username);

    AdminUserResponseDto getByEmail(String email);

    List<AdminUserResponseDto> getAll();

    List<AdminUserResponseDto> getAllActiveUsers();

    List<AdminUserResponseDto> getAllInactiveUsers();

    Page<AdminUserResponseDto> search(
            String username,
            String email,
            RoleName roleName,
            Boolean active,
            Pageable pageable
    );

    void addUser(CreateUserRequestDto userRequestDto);

    void deactivateUser(Long currentAdminId, Long userId, String reason);

    void activateUser(Long currentAdminId, Long userId, String reason);

    void updateUserProfileByAdmin(Long currentAdminId, Long id, UpdateUserProfileRequestDtoByAdmin userProfileRequestDto);

    void resetPassword(Long currentAdminId, Long id, ResetPasswordRequestDto resetPasswordRequestDto);

    void updateRole(Long currentAdminId, Long id, UpdateUserRoleRequestDto updateUserRoleRequestDto);
}
