package com.supplog.service.user.impl;

import com.supplog.dto.user.*;
import com.supplog.entity.User;
import com.supplog.exception.BusinessException;
import com.supplog.exception.ResourceNotFoundException;
import com.supplog.repository.UserRepository;
import com.supplog.service.user.UserService;
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


        User user = findUserById(id);
        return mapper.map(user, UserResponseDto.class);
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
    public List<UserResponseDto> getAllActiveUsers() {

        List<User> users = userRepository.findAllByIsDeletedFalse();
        List<UserResponseDto> userResponseDtos = new ArrayList<>();

        for (User user : users) {
            userResponseDtos.add(mapper.map(user, UserResponseDto.class));
        }

        return userResponseDtos;
    }

    @Override
    public void addUser(CreateUserRequestDto userRequestDto) {
        if (userRepository.findByEmail(userRequestDto.getEmail()).isPresent()) {
            throw new BusinessException("user.email.already.exists");
        }

        if (userRepository.findByUsername(userRequestDto.getUsername()).isPresent()) {
            throw new BusinessException("user.username.already.exists");
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
    public void changePasswordById(Long id, ChangePasswordRequestDto passwordRequestDto) {
        changePassword(findUserById(id), passwordRequestDto);

    }

    @Override
    public void updateUserInfoByEmail(String email, UpdateUserProfileRequestDto userProfileRequestDto) {
        updateUserInfo(findUserByEmail(email), userProfileRequestDto);
    }

    @Override
    public void updateUserInfoById(Long id, UpdateUserProfileRequestDto userProfileRequestDto) {
        updateUserInfo(findUserById(id), userProfileRequestDto);

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
        String userRequestDtoPassword = userRequestDto.getPassword();

        validatePassword(user, userRequestDtoPassword);

        user.setDeleted(true);
        userRepository.save(user);
    }

    @Override
    public void deleteUserById(Long id, DeleteUserRequestDto userRequestDto) {
        User user = findUserById(id);
        String userRequestDtoPassword = userRequestDto.getPassword();

        validatePassword(user, userRequestDtoPassword);

        user.setDeleted(true);
        userRepository.save(user);


    }


    //HELPER METHODS
    private void validatePassword(User user, String password) {
        if (!user.getPassword().equals(password)) {
            throw new BusinessException("user.password.incorrect");
        }
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("user.email.not.found", email));
    }

    private User findUserByUserName(String userName) {
        return userRepository.findByUsername(userName)
                .orElseThrow(() -> new ResourceNotFoundException("user.username.not.found", userName));
    }

    private User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("user.not.found", id));
    }

    private void changePassword(User user, ChangePasswordRequestDto requestDto) {
        validatePassword(user, requestDto.getOldPassword());

        if (!requestDto.getNewPassword().equals(requestDto.getConfirmPassword())) {
            throw new BusinessException("user.password.not.match");
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
