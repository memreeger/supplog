package com.supplog.service.user.impl;

import com.supplog.dto.user.*;
import com.supplog.entity.User;
import com.supplog.repository.UserRepository;
import com.supplog.service.user.UserService;
import jakarta.persistence.EntityNotFoundException;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final ModelMapper mapper;

    public UserServiceImpl(UserRepository userRepository, ModelMapper mapper) {
        this.userRepository = userRepository;
        this.mapper = mapper;
    }


    @Override
    public UserResponseDto getById(Long id) {


        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        UserResponseDto dto = mapper.map(user, UserResponseDto.class);
        return dto;
    }

    @Override
    public UserResponseDto getByUserName(String userName) {
        User user = findUserByUserName(userName);
        return mapper.map(user, UserResponseDto.class);
    }

    @Override
    public UserResponseDto getByEmail(String email) {

        User user = findUserByEmail(email);
        return mapper.map(user, UserResponseDto.class);
    }

    @Override
    public List<UserResponseDto> getAll() {

        List<User> users = userRepository.findAll();

        List<UserResponseDto> userResponseDtos = new ArrayList<>();

        for (User user : users) {
            userResponseDtos.add(
                    mapper.map(user, UserResponseDto.class)
            );
        }

        return userResponseDtos;
    }

    @Override
    public void addUser(CreateUserRequestDto userRequestDto) {
        if (userRepository.findByEmail(userRequestDto.getEmail()).isPresent()) {
            throw new IllegalArgumentException("This email already exists");
        }

        if (userRepository.findByUserName(userRequestDto.getUserName()).isPresent()) {
            throw new IllegalArgumentException("This user name already exists");
        }
        User user = new User();
        mapper.map(userRequestDto, user);
        userRepository.save(user);
    }

    @Override
    public void changePasswordByEmail(String email, ChangePasswordRequestDto requestDto) {
        changePassword(findUserByEmail(email), requestDto);
    }

    @Override
    public void updateUserInfoByEmail(String email, UpdateUserProfileRequestDto userProfileRequestDto) {
        updateUserInfo(findUserByEmail(email), userProfileRequestDto);
    }

    @Override
    public void changePasswordByUserName(String userName, ChangePasswordRequestDto passwordRequestDto) {
        changePassword(findUserByUserName(userName), passwordRequestDto);
    }

    @Override
    public void updateUserInfoByUserName(String userName, UpdateUserProfileRequestDto userProfileRequestDto) {
        updateUserInfo(findUserByUserName(userName), userProfileRequestDto);
    }

    @Override
    public void deleteUserByEmail(String email, DeleteUserRequestDto userRequestDto) {
        User user = findUserByEmail(email);

        if (!user.getPassword().equals(userRequestDto.getPassword())) {
            throw new IllegalArgumentException("Password is incorrect");
        }

        user.setDeleted(true);
        userRepository.save(user);
    }


    //HELPER METHODS
    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User email not found"));
    }

    private User findUserByUserName(String userName) {
        return userRepository.findByUserName(userName)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private void changePassword(User user, ChangePasswordRequestDto requestDto) {
        if (!user.getPassword().equals(requestDto.getOldPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        if (!requestDto.getNewPassword().equals(requestDto.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        user.setPassword(requestDto.getNewPassword());
        userRepository.save(user);
    }

    private void updateUserInfo(User user, UpdateUserProfileRequestDto requestDto) {
        user.setFirstName(requestDto.getFirstName());
        user.setLastName(requestDto.getLastName());
        userRepository.save(user);
    }
}
