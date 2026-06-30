package com.supplog.service.user;

import com.supplog.dto.user.*;


public interface UserService {

    UserResponseDto getMyProfile(String username);

    void changeMyPassword(String username, ChangePasswordRequestDto changePasswordRequestDto);

    void updateMyProfile(String username, UpdateUserProfileRequestDto userProfileRequestDto);

    void deActivateMyProfile(String username, DeleteUserRequestDto deleteUserRequestDto);

}
