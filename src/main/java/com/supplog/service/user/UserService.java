package com.supplog.service.user;

import com.supplog.dto.user.*;


public interface UserService {


    UserResponseDto getMyProfile(Long id);


    void changeMyPassword(Long id, ChangePasswordRequestDto changePasswordRequestDto);


    void updateMyProfile(Long id, UpdateUserProfileRequestDto userProfileRequestDto);


    void deActivateMyProfile(Long id, DeleteUserRequestDto deleteUserRequestDto);

    void updateMyTimeZone(
            Long userId,
            UpdateTimeZoneRequestDto request
    );

}
