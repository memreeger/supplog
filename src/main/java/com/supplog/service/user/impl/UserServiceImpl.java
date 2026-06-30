package com.supplog.service.user.impl;

import com.supplog.dto.user.*;
import com.supplog.entity.User;
import com.supplog.exception.BusinessException;
import com.supplog.exception.ResourceNotFoundException;
import com.supplog.repository.UserRepository;
import com.supplog.service.user.UserService;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, ModelMapper modelMapper, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.modelMapper = modelMapper;
        this.passwordEncoder = passwordEncoder;
    }


    @Override
    public UserResponseDto getMyProfile(String username) {
        User user = findUserByUsername(username);
        return modelMapper.map(user, UserResponseDto.class);
    }

    @Override
    public void changeMyPassword(String username, ChangePasswordRequestDto changePasswordRequestDto) {
        User user = findUserByUsername(username);

        if (!passwordEncoder.matches(changePasswordRequestDto.getOldPassword(), user.getPassword())) {
            throw new BusinessException("user.old.password.incorrect");
        }

        if (!changePasswordRequestDto.getNewPassword().equals(changePasswordRequestDto.getConfirmPassword())) {
            throw new BusinessException("user.password.not.match");
        }

        user.setPassword(passwordEncoder.encode(changePasswordRequestDto.getNewPassword()));
        userRepository.save(user);

    }

    @Override
    public void updateMyProfile(String username, UpdateUserProfileRequestDto userProfileRequestDto) {
        User user = findUserByUsername(username);
        user.setFirstName(userProfileRequestDto.getFirstName());
        user.setLastName(userProfileRequestDto.getLastName());
        userRepository.save(user);

    }

    @Override
    public void deActivateMyProfile(String username, DeleteUserRequestDto deleteUserRequestDto) {
        User user = findUserByUsername(username);

        if (passwordEncoder.matches(deleteUserRequestDto.getPassword(), user.getPassword())) {
            throw new BusinessException("user.password.not.match");
        }
        user.setDeleted(true);
        userRepository.save(user);

    }


    //HELPER
    private User findUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("user.not.found"));
    }

}
