package com.supplog.service.user;

import com.supplog.dto.user.*;

import java.util.List;

public interface UserService {


    UserResponseDto getById(Long id);

    UserResponseDto getByUserName(String username);

    UserResponseDto getByEmail(String email);

    List<UserResponseDto> getAll();

    List<UserResponseDto> getAllActiveUsers();

    void addUser(CreateUserRequestDto userRequestDto);

    void changePasswordByEmail(String email, ChangePasswordRequestDto passwordRequestDto);

    void changePasswordById(Long id, ChangePasswordRequestDto passwordRequestDto);

    void updateUserInfoByEmail(String email, UpdateUserProfileRequestDto userProfileRequestDto);

    void updateUserInfoById(Long id, UpdateUserProfileRequestDto userProfileRequestDto);

    void changePasswordByUserName(String userName, ChangePasswordRequestDto passwordRequestDto);

    void updateUserInfoByUserName(String userName, UpdateUserProfileRequestDto userProfileRequestDto);

    void deleteUserByEmail(String email, DeleteUserRequestDto userRequestDto);

    void deleteUserById(Long id, DeleteUserRequestDto userRequestDto);

    // change password spring security / jwt öğrenince sadece tek parametre ile yaz

    // change ad soyad

}
