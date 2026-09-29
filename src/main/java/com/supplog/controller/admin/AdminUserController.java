package com.supplog.controller.admin;

import com.supplog.dto.admin.user.AdminUserResponseDto;
import com.supplog.dto.admin.user.ResetPasswordRequestDto;
import com.supplog.dto.admin.user.UpdateUserProfileRequestDtoByAdmin;
import com.supplog.dto.admin.user.UpdateUserRoleRequestDto;
import com.supplog.dto.admin.user.AdminUserDetailResponseDto;
import com.supplog.dto.admin.AdminReasonRequestDto;
import com.supplog.enums.RoleName;
import com.supplog.dto.user.CreateUserRequestDto;
import com.supplog.dto.user.UpdateUserProfileRequestDto;
import com.supplog.service.admin.adminUserService.AdminUserService;
import com.supplog.service.user.impl.CustomUserDetails;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {
    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    //Tüm kullanıcıları listele
    @GetMapping
    List<AdminUserResponseDto> getAllUsers(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) RoleName role,
            @RequestParam(required = false) Boolean active
    ) {
        return adminUserService.search(username, email, role, active);
    }

    @GetMapping("/{id}/detail")
    AdminUserDetailResponseDto getDetail(
            @PathVariable @Positive(message = "{validation.id.positive}") Long id
    ) {
        return adminUserService.getDetail(id);
    }


    //Id ile kullanıcı getir
    @GetMapping("/{id}")
    AdminUserResponseDto getById(@PathVariable
                            @Positive(message = "{validation.id.positive}") Long id) {
        return adminUserService.getById(id);
    }

    //Username ile kullanıcı ara
    @GetMapping("/search/username/{username}")
    AdminUserResponseDto getByUsername(@PathVariable String username) {
        return adminUserService.getByUserName(username);
    }

    //Email ile kullanıcı ara
    @GetMapping("/search/email/{email}")
    AdminUserResponseDto getByEmail(@PathVariable String email) {
        return adminUserService.getByEmail(email);
    }


    //Aktif kullanıcıları getir
    @GetMapping("/activeUsers")
    List<AdminUserResponseDto> getActiveUsers() {
        return adminUserService.getAllActiveUsers();
    }


    //Pasif kullanıcıları getir
    @GetMapping("/inactiveUsers")
    List<AdminUserResponseDto> getInActiveUsers() {
        return adminUserService.getAllInactiveUsers();
    }


    //Yeni kullanıcı oluştur
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    void createUser(@Valid @RequestBody CreateUserRequestDto createUserRequestDto) {
        adminUserService.addUser(createUserRequestDto);
    }

    //kullanıcıyı deactive et
    @PatchMapping("/{userId}/deactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deActivateUser(
            @AuthenticationPrincipal CustomUserDetails currentAdmin,
            @PathVariable @Positive(message = "{validation.id.positive}") Long userId,
            @Valid @RequestBody AdminReasonRequestDto request
    ) {
        adminUserService.deactivateUser(currentAdmin.getId(), userId, request.reason());
    }

    //kullanıcıyı active et
    @PatchMapping("/{id}/activate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void activateUser(
            @AuthenticationPrincipal CustomUserDetails currentAdmin,
            @PathVariable @Positive(message = "{validation.id.positive}") Long id,
            @Valid @RequestBody AdminReasonRequestDto request
    ) {
        adminUserService.activateUser(currentAdmin.getId(), id, request.reason());
    }


    //Kullanıcı bilgilerini güncelle
    // KULLANICI UPDATE İÇİN ADMİN DTO OLUŞTUR
    @PutMapping("/{id}/updateProfile")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateProfileByAdmin(
            @AuthenticationPrincipal CustomUserDetails currentAdmin,
            @PathVariable @Positive(message = "{validation.id.positive}") Long id,
            @Valid @RequestBody UpdateUserProfileRequestDtoByAdmin updateUserProfileRequestDto
    ) {
        adminUserService.updateUserProfileByAdmin(currentAdmin.getId(), id, updateUserProfileRequestDto);
    }

    //Kullanıcı şifresini değiştir
    @PostMapping("/{id}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPasswordByAdmin(
            @AuthenticationPrincipal CustomUserDetails currentAdmin,
            @PathVariable @Positive(message = "{validation.id.positive}") Long id,
            @Valid @RequestBody ResetPasswordRequestDto resetPasswordRequestDto
    ) {
        adminUserService.resetPassword(currentAdmin.getId(), id, resetPasswordRequestDto);
    }

    //Kullanıcının rolünü değiştir
    @PutMapping("/{id}/role")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateRoleByAdmin(@AuthenticationPrincipal CustomUserDetails currentAdmin, @PathVariable
    @Positive(message = "{validation.id.positive}") Long id,@Valid @RequestBody UpdateUserRoleRequestDto updateUserRoleRequestDto) {
        adminUserService.updateRole(currentAdmin.getId(), id, updateUserRoleRequestDto);
    }

}
