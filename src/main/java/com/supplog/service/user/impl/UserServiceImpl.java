package com.supplog.service.user.impl;

import com.supplog.dto.user.*;
import com.supplog.entity.User;
import com.supplog.enums.RoleName;
import com.supplog.exception.BusinessException;
import com.supplog.exception.ResourceNotFoundException;
import com.supplog.repository.UserRepository;
import com.supplog.service.support.SupportService;
import com.supplog.service.user.UserService;
import com.supplog.util.InputNormalizer;
import com.supplog.util.TimeZoneResolver;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
//@Primary
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;
    private final TimeZoneResolver timeZoneResolver;
    private final SupportService supportService;

    public UserServiceImpl(UserRepository userRepository,
                           ModelMapper modelMapper,
                           PasswordEncoder passwordEncoder,
                           TimeZoneResolver timeZoneResolver,
                           SupportService supportService) {

        this.userRepository = userRepository;
        this.modelMapper = modelMapper;
        this.passwordEncoder = passwordEncoder;
        this.timeZoneResolver = timeZoneResolver;
        this.supportService = supportService;
    }


    @Override
    public UserResponseDto getMyProfile(Long id) {
        User user = findActiveUserById(id);
        return modelMapper.map(user, UserResponseDto.class);
    }


    @Override
    @Transactional
    public void changeMyPassword(Long id, ChangePasswordRequestDto changePasswordRequestDto) {
        User user = findActiveUserById(id);
        if (!passwordEncoder.matches(changePasswordRequestDto.getOldPassword(), user.getPassword())) {
            throw new BusinessException("user.old.password.incorrect");
        }

        if (!changePasswordRequestDto.getNewPassword().equals(changePasswordRequestDto.getConfirmPassword())) {
            throw new BusinessException("user.password.not.match");
        }

        if (passwordEncoder.matches(changePasswordRequestDto.getNewPassword(), user.getPassword())) {
            throw new BusinessException("user.password.must.be.different");
        }

        user.setTokenVersion(user.getTokenVersion() + 1);
        user.setPassword(passwordEncoder.encode(changePasswordRequestDto.getNewPassword()));
    }


    @Override
    @Transactional
    public void updateMyProfile(Long id, UpdateUserProfileRequestDto userProfileRequestDto) {
        User user = findActiveUserById(id);
        user.setFirstName(InputNormalizer.trim(userProfileRequestDto.getFirstName()));
        user.setLastName(InputNormalizer.trim(userProfileRequestDto.getLastName()));

    }


    @Override
    @Transactional
    public void deActivateMyProfile(Long id, DeleteUserRequestDto deleteUserRequestDto) {
        User user = findActiveUserById(id);
        boolean isAdmin = user.getRoles()
                .stream()
                .anyMatch(role -> role.getName() == RoleName.ROLE_ADMIN);

        if (isAdmin) {
            throw new BusinessException("admin.cannot.deactivate.self");
        }

        if (!passwordEncoder.matches(deleteUserRequestDto.getPassword(), user.getPassword())) {
            throw new BusinessException("user.password.incorrect");
        }


        supportService.handleUserDeactivation(id);

        user.setTokenVersion(user.getTokenVersion() + 1);
        user.setDeleted(true);

    }

    @Override
    @Transactional
    public void updateMyTimeZone(
            Long userId,
            UpdateTimeZoneRequestDto request
    ) {

        User user = findActiveUserById(userId);

        user.setTimeZone(
                timeZoneResolver.normalize(
                        request.getTimeZone()
                )
        );
    }


    //HELPER

    private User findActiveUserById(Long userId) {
        return userRepository
                .findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "user.not.found",
                                userId
                        )
                );
    }

}
