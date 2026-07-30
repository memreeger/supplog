package com.supplog.service.admin.adminUserService.impl;


import com.supplog.dto.admin.user.ResetPasswordRequestDto;
import com.supplog.dto.admin.user.UpdateUserRoleDto;
import com.supplog.dto.user.CreateUserRequestDto;
import com.supplog.dto.user.UpdateUserProfileRequestDto;
import com.supplog.dto.user.UserResponseDto;
import com.supplog.entity.Role;
import com.supplog.entity.User;
import com.supplog.enums.RoleName;
import com.supplog.exception.BusinessException;
import com.supplog.exception.ResourceNotFoundException;
import com.supplog.repository.RoleRepository;
import com.supplog.repository.RoutineRepository;
import com.supplog.repository.SupplementRepository;
import com.supplog.repository.UserRepository;
import com.supplog.service.admin.adminUserService.AdminUserService;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class AdminUserServiceImpl implements AdminUserService {
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final SupplementRepository supplementRepository;
    private final RoutineRepository routineRepository;

    // 104. satır validasyon ekle message olarak da ekle

    public AdminUserServiceImpl(UserRepository userRepository, ModelMapper modelMapper, PasswordEncoder passwordEncoder, RoleRepository roleRepository, SupplementRepository supplementRepository, RoutineRepository routineRepository) {
        this.userRepository = userRepository;
        this.modelMapper = modelMapper;
        this.passwordEncoder = passwordEncoder;
        this.roleRepository = roleRepository;
        this.supplementRepository = supplementRepository;
        this.routineRepository = routineRepository;
    }


    @Override
    public UserResponseDto getById(Long id) {


        User user = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("user.not.found",id));
        return modelMapper.map(user, UserResponseDto.class);
    }


    @Override
    public UserResponseDto getByUserName(String userName) {
        String normalizedUsername = userName.trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByUsername(normalizedUsername).orElseThrow(() -> new ResourceNotFoundException("user.not.found"));
        return modelMapper.map(user, UserResponseDto.class);
    }

    @Override
    public UserResponseDto getByEmail(String email) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmail(normalizedEmail).orElseThrow(() -> new ResourceNotFoundException("user.not.found"));
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
        String username = userRequestDto.getUsername().trim().toLowerCase(Locale.ROOT);
        String email = userRequestDto.getEmail().trim().toLowerCase(Locale.ROOT);

        if (userRepository.findByEmail(email).isPresent()) {
            throw new BusinessException("user.email.already.exists");
        }

        if (userRepository.findByUsername(username).isPresent()) {
            throw new BusinessException("user.username.already.exists");
        }

        Role role = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseThrow(() ->new ResourceNotFoundException("role.not.found"));

        User user = new User();
        modelMapper.map(userRequestDto, user);

        user.setUsername(username);
        user.setEmail(email);
        user.setFirstName(userRequestDto.getFirstName().trim());
        user.setLastName(userRequestDto.getLastName().trim());
        user.getRoles().add(role);
        user.setPassword(passwordEncoder.encode(userRequestDto.getPassword()));

        userRepository.save(user);
    }

    @Override
    @Transactional
    public void deactivateUser(Long currentAdminId, Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("user.not.found"));
        if (user.isDeleted()) {
            throw new BusinessException("user.already.inactive");
        }

        if (currentAdminId.equals(userId)) {
            throw new BusinessException("admin.cannot.deactivate.self");
        }

        boolean targetIsAdmin = user.getRoles()
                .stream()
                .anyMatch(role -> role.getName() == RoleName.ROLE_ADMIN);

        if (targetIsAdmin) {
            long activeAdminCount = userRepository.countActiveUsersByRole(RoleName.ROLE_ADMIN);
            if (activeAdminCount <= 1) {
                throw new BusinessException("admin.last.active.cannot.deactivate");
            }
        }

        routineRepository.softDeleteAllByUserId(userId);
        supplementRepository.softDeleteAllByUserId(userId);

        user.setTokenVersion(user.getTokenVersion() + 1);
        user.setDeleted(true);
    }

    @Override
    public void activateUser(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("user.not.found",userId));
        if(!user.isDeleted()){
            throw new BusinessException("user.already.active");
        }
        user.setDeleted(false);
        userRepository.save(user);
    }

    //Genişletilecek ve updateProileByAdmin için DTO oluşturulacak
    @Override
    public void updateUserProfileByAdmin(Long id, UpdateUserProfileRequestDto userProfileRequestDto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("user.not.found",id));

        user.setFirstName(userProfileRequestDto.getFirstName().trim());
        user.setLastName(userProfileRequestDto.getLastName().trim());

        userRepository.save(user);
    }

    @Override
    public void resetPassword(Long id, ResetPasswordRequestDto request) {
        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("user.not.found",id));

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("user.password.not.match");
        }

        if(passwordEncoder.matches(
                request.getNewPassword(),
                user.getPassword())){
            throw new BusinessException("user.password.must.be.different");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void updateRole(
            Long currentAdminId,
            Long id,
            UpdateUserRoleDto updateUserRoleDto
    ) {
        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("user.not.found",id)
                );

        RoleName requestedRoleName =
                updateUserRoleDto.getRoleName();

        boolean isSameRole = user.getRoles()
                .stream()
                .anyMatch(role ->
                        role.getName() == requestedRoleName
                );

        if (isSameRole) {
            throw new BusinessException("user.role.is.same");
        }

        boolean targetIsAdmin = user.getRoles()
                .stream()
                .anyMatch(role ->
                        role.getName() == RoleName.ROLE_ADMIN
                );

        boolean removingAdminRole =
                !user.isDeleted()
                        && targetIsAdmin
                        && requestedRoleName != RoleName.ROLE_ADMIN;

        if (currentAdminId.equals(id) && removingAdminRole) {
            throw new BusinessException(
                    "admin.cannot.change.own.role"
            );
        }

        if (removingAdminRole) {
            long activeAdminCount =
                    userRepository.countActiveUsersByRole(
                            RoleName.ROLE_ADMIN
                    );

            if (activeAdminCount <= 1) {
                throw new BusinessException(
                        "admin.last.active.role.cannot.change"
                );
            }
        }

        Role role = roleRepository
                .findByName(requestedRoleName)
                .orElseThrow(() ->
                        new ResourceNotFoundException("role.not.found",id)
                );

        user.getRoles().clear();
        user.getRoles().add(role);
    }
}
