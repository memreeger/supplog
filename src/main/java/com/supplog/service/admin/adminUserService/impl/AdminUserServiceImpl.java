package com.supplog.service.admin.adminUserService.impl;


import com.supplog.dto.admin.user.AdminUserResponseDto;
import com.supplog.dto.admin.user.ResetPasswordRequestDto;
import com.supplog.dto.admin.user.UpdateUserProfileRequestDtoByAdmin;
import com.supplog.dto.admin.user.UpdateUserRoleRequestDto;
import com.supplog.dto.user.CreateUserRequestDto;
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
import com.supplog.service.support.SupportService;
import com.supplog.util.InputNormalizer;
import com.supplog.util.TimeZoneResolver;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AdminUserServiceImpl implements AdminUserService {
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final TimeZoneResolver timeZoneResolver;
    private final SupportService supportService;


    public AdminUserServiceImpl(UserRepository userRepository,
                                ModelMapper modelMapper,
                                PasswordEncoder passwordEncoder,
                                RoleRepository roleRepository,
                                TimeZoneResolver timeZoneResolver,
                                SupportService supportService) {
        this.userRepository = userRepository;
        this.modelMapper = modelMapper;
        this.passwordEncoder = passwordEncoder;
        this.roleRepository = roleRepository;
        this.timeZoneResolver = timeZoneResolver;
        this.supportService = supportService;
    }


    @Override
    public AdminUserResponseDto getById(Long id) {


        User user = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("user.not.found", id));
        return toAdminUserResponseDto(user);
    }


    @Override
    public AdminUserResponseDto getByUserName(String userName) {
        String normalizedUsername = InputNormalizer.normalizeUsername(userName);
        User user = userRepository.findByUsername(normalizedUsername)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "user.username.not.found",
                                normalizedUsername
                        )
                );
        return toAdminUserResponseDto(user);
    }

    @Override
    public AdminUserResponseDto getByEmail(String email) {
        String normalizedEmail = InputNormalizer.normalizeEmail(email);

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "user.email.not.found",
                                normalizedEmail
                        )
                );
        return toAdminUserResponseDto(user);
    }

    @Override
    public List<AdminUserResponseDto> getAll() {
        List<User> allUsers = userRepository.findAll();
        List<AdminUserResponseDto> allUserDtos = new ArrayList<>();

        for (User user : allUsers) {
            allUserDtos.add(toAdminUserResponseDto(user));
        }
        return allUserDtos;

    }

    @Override
    public List<AdminUserResponseDto> getAllActiveUsers() {
        List<User> allActiveUsers = userRepository.findAllByIsDeletedFalse();
        List<AdminUserResponseDto> allActiveUserResponseDtos = new ArrayList<>();
        for (User user : allActiveUsers) {
            allActiveUserResponseDtos.add(toAdminUserResponseDto(user));
        }
        return allActiveUserResponseDtos;
    }

    @Override
    public List<AdminUserResponseDto> getAllInactiveUsers() {
        List<User> allInactiveUsers = userRepository.findAllByIsDeletedTrue();
        List<AdminUserResponseDto> allInactiveUserResponseDtos = new ArrayList<>();
        for (User user : allInactiveUsers) {
            allInactiveUserResponseDtos.add(toAdminUserResponseDto(user));
        }
        return allInactiveUserResponseDtos;
    }

    @Override
    @Transactional
    public void addUser(CreateUserRequestDto userRequestDto) {
        String username = InputNormalizer.normalizeUsername(userRequestDto.getUsername());
        String email = InputNormalizer.normalizeEmail(userRequestDto.getEmail());

        if (userRepository.findByEmail(email).isPresent()) {
            throw new BusinessException("user.email.already.exists");
        }

        if (userRepository.findByUsername(username).isPresent()) {
            throw new BusinessException("user.username.already.exists");
        }

        Role role = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseThrow(() -> new ResourceNotFoundException("role.not.found"));

        User user = new User();
        modelMapper.map(userRequestDto, user);

        user.setUsername(username);
        user.setEmail(email);
        user.setFirstName(InputNormalizer.trim(userRequestDto.getFirstName()));
        user.setLastName(InputNormalizer.trim(userRequestDto.getLastName()));
        user.getRoles().add(role);
        user.setPassword(passwordEncoder.encode(userRequestDto.getPassword()));
        user.setTimeZone(timeZoneResolver.normalize(userRequestDto.getTimeZone()));

        userRepository.save(user);
    }

    @Override
    @Transactional
    public void deactivateUser(Long currentAdminId, Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("user.not.found", userId));
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

        supportService.handleUserDeactivation(userId);

        user.setTokenVersion(user.getTokenVersion() + 1);
        user.setDeleted(true);
    }

    @Override
    @Transactional
    public void activateUser(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("user.not.found", userId));
        if (!user.isDeleted()) {
            throw new BusinessException("user.already.active");
        }
        user.setDeleted(false);
    }

    //Genişletilecek ve updateProileByAdmin için DTO oluşturulacak
    @Override
    @Transactional
    public void updateUserProfileByAdmin(Long id, UpdateUserProfileRequestDtoByAdmin userProfileRequestDto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("user.not.found", id));

        user.setUsername(
                InputNormalizer.normalizeUsername(
                        userProfileRequestDto.getUsername()
                )
        );
        user.setFirstName(InputNormalizer.trim(userProfileRequestDto.getFirstName()));
        user.setLastName(InputNormalizer.trim(userProfileRequestDto.getLastName()));
        user.setEmail(InputNormalizer.normalizeEmail(userProfileRequestDto.getEmail()));
        user.setBirthDate(userProfileRequestDto.getBirthDate());


    }

    @Override
    @Transactional
    public void resetPassword(Long id, ResetPasswordRequestDto request) {
        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("user.not.found", id));

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("user.password.not.match");
        }

        if (passwordEncoder.matches(
                request.getNewPassword(),
                user.getPassword())) {
            throw new BusinessException("user.password.must.be.different");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setTokenVersion(user.getTokenVersion() + 1);
    }

    @Override
    @Transactional
    public void updateRole(
            Long currentAdminId,
            Long id,
            UpdateUserRoleRequestDto updateUserRoleRequestDto
    ) {
        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "user.not.found",
                                id
                        )
                );

        RoleName requestedRoleName =
                updateUserRoleRequestDto.getRoleName();

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
                        new ResourceNotFoundException(
                                "role.not.found"
                        )
                );

        user.getRoles().clear();
        user.getRoles().add(role);
    }

    private AdminUserResponseDto toAdminUserResponseDto(User user) {

        AdminUserResponseDto dto =
                modelMapper.map(user, AdminUserResponseDto.class);

        dto.setDeleted(user.isDeleted());

        dto.setRoles(
                user.getRoles()
                        .stream()
                        .map(Role::getName)
                        .collect(java.util.stream.Collectors.toSet())
        );

        return dto;
    }
}
