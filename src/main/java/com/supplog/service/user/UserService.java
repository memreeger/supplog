package com.supplog.service.user;

import com.supplog.dto.user.*;
import org.springframework.security.core.userdetails.UserDetailsService;


//public interface UserService extends UserDetailsService {
public interface UserService {

    UserResponseDto getMyProfile(String username);

    UserResponseDto getMyProfile(Long id);

    void changeMyPassword(String username, ChangePasswordRequestDto changePasswordRequestDto);

    void changeMyPassword(Long id, ChangePasswordRequestDto changePasswordRequestDto);

    void updateMyProfile(String username, UpdateUserProfileRequestDto userProfileRequestDto);

    void updateMyProfile(Long id, UpdateUserProfileRequestDto userProfileRequestDto);

    void deActivateMyProfile(String username, DeleteUserRequestDto deleteUserRequestDto);

    void deActivateMyProfile(Long id, DeleteUserRequestDto deleteUserRequestDto);

}
