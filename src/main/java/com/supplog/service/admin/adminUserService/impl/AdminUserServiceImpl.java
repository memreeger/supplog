package com.supplog.service.admin.adminUserService.impl;


import com.supplog.dto.admin.user.ResetPasswordRequestDto;
import com.supplog.dto.user.CreateUserRequestDto;
import com.supplog.dto.user.UpdateUserProfileRequestDto;
import com.supplog.dto.user.UserResponseDto;
import com.supplog.entity.Role;
import com.supplog.entity.User;
import com.supplog.enums.RoleName;
import com.supplog.exception.BusinessException;
import com.supplog.exception.ResourceNotFoundException;
import com.supplog.repository.RoleRepository;
import com.supplog.repository.UserRepository;
import com.supplog.service.admin.adminUserService.AdminUserService;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class AdminUserServiceImpl implements AdminUserService {
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;

    // 104. satır validasyon ekle message olarak da ekle

    public AdminUserServiceImpl(UserRepository userRepository, ModelMapper modelMapper, PasswordEncoder passwordEncoder, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.modelMapper = modelMapper;
        this.passwordEncoder = passwordEncoder;
        this.roleRepository = roleRepository;
    }


    @Override
    public UserResponseDto getById(Long id) {


        User user = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("user.not.found"));
        return modelMapper.map(user, UserResponseDto.class);
    }


    @Override
    public UserResponseDto getByUserName(String userName) {
        User user = userRepository.findByUsername(userName).orElseThrow(() -> new ResourceNotFoundException("user.not.found"));
        return modelMapper.map(user, UserResponseDto.class);
    }

    @Override
    public UserResponseDto getByEmail(String email) {

        User user = userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("user.not.found"));
        return modelMapper.map(user, UserResponseDto.class);
    }

    @Override
    public List<UserResponseDto> getAll() {
        List<User> allUsers = userRepository.findAll();
        List<UserResponseDto> allUserDtos = new ArrayList<>();

        for (User user : allUsers) {
            allUserDtos.add(modelMapper.map(user, UserResponseDto.class));
        }
        return allUserDtos;

    }

    @Override
    public List<UserResponseDto> getAllActiveUsers() {
        List<User> allActiveUsers = userRepository.findAllByIsDeletedFalse();
        List<UserResponseDto> allActiveUserResponseDtos = new ArrayList<>();
        for (User user : allActiveUsers) {
            allActiveUserResponseDtos.add(modelMapper.map(user, UserResponseDto.class));
        }
        return allActiveUserResponseDtos;
    }

    @Override
    public List<UserResponseDto> getAllInactiveUsers() {
        List<User> allInactiveUsers = userRepository.findAllByIsDeletedTrue();
        List<UserResponseDto> allInactiveUserResponseDtos = new ArrayList<>();
        for (User user : allInactiveUsers) {
            allInactiveUserResponseDtos.add(modelMapper.map(user, UserResponseDto.class));
        }
        return allInactiveUserResponseDtos;
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
        modelMapper.map(userRequestDto, user);
        Role role = roleRepository.findByName(RoleName.ROLE_USER).orElseThrow();
        user.setRoles(Set.of(role));
        user.setPassword(passwordEncoder.encode(userRequestDto.getPassword()));
        userRepository.save(user);
    }

    @Override
    public void deactivateUser(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("user.not.found"));
        user.setDeleted(true);
        userRepository.save(user);
    }

    @Override
    public void activateUser(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("user.not.found"));
        user.setDeleted(false);
        userRepository.save(user);
    }

    //Genişletilecek ve updateProileByAdmin için DTO oluşturulacak
    @Override
    public void updateUserProfileByAdmin(Long id, UpdateUserProfileRequestDto userProfileRequestDto) {
        User user = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("user.not.found"));
        user.setFirstName(userProfileRequestDto.getFirstName());
        user.setLastName(userProfileRequestDto.getLastName());
        userRepository.save(user);
    }

    @Override
    public void resetPassword(Long id, ResetPasswordRequestDto request) {
        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("user.not.found"));

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("user.password.not.match");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
}
