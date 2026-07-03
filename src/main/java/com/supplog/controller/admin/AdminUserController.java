package com.supplog.controller.admin;

import com.supplog.dto.admin.user.ResetPasswordRequestDto;
import com.supplog.dto.user.CreateUserRequestDto;
import com.supplog.dto.user.UpdateUserProfileRequestDto;
import com.supplog.dto.user.UserResponseDto;
import com.supplog.service.admin.adminUserService.AdminUserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
    List<UserResponseDto> getAllUsers() {
        return adminUserService.getAll();
    }


    //Id ile kullanıcı getir
    @GetMapping("/{id}")
    UserResponseDto getById(@PathVariable Long id) {
        return adminUserService.getById(id);
    }

    //Username ile kullanıcı ara
    @GetMapping("/search/username/{username}")
    UserResponseDto getByUsername(@PathVariable String username) {
        return adminUserService.getByUserName(username);
    }

    //Email ile kullanıcı ara
    @GetMapping("/search/email/{email}")
    UserResponseDto getByEmail(@PathVariable String email) {
        return adminUserService.getByEmail(email);
    }


    //Aktif kullanıcıları getir
    @GetMapping("/activeUsers")
    List<UserResponseDto> getActiveUsers() {
        return adminUserService.getAllActiveUsers();
    }


    //Pasif kullanıcıları getir
    @GetMapping("/inactiveUsers")
    List<UserResponseDto> getInActiveUsers() {
        return adminUserService.getAllInactiveUsers();
    }


    //Yeni kullanıcı oluştur
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    void createUser(@Valid @RequestBody CreateUserRequestDto createUserRequestDto) {
        adminUserService.addUser(createUserRequestDto);
    }

    //kullanıcıyı deactive et
    @PatchMapping("/{id}/deactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deActivateUser(@PathVariable Long id) {
        adminUserService.deactivateUser(id);
    }

    //kullanıcıyı active et
    @PatchMapping("/{id}/activate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void activateUser(@PathVariable Long id) {
        adminUserService.activateUser(id);
    }


    //Kullanıcı bilgilerini güncelle
    // KULLANICI UPDATE İÇİN ADMİN DTO OLUŞTUR
    @PutMapping("/{id}/updateProfile")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateProfileByAdmin(@PathVariable Long id, @Valid @RequestBody UpdateUserProfileRequestDto updateUserProfileRequestDto) {
        adminUserService.updateUserProfileByAdmin(id, updateUserProfileRequestDto);
    }

    //Kullanıcı şifresini değiştir
    @PostMapping("/{id}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPasswordByAdmin(@PathVariable Long id, @Valid @RequestBody ResetPasswordRequestDto resetPasswordRequestDto) {
        adminUserService.resetPassword(id, resetPasswordRequestDto);
    }

    //Kullanıcının rolünü değiştir
    //@PutMapping("/{id}/role")

}
