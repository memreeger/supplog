package com.supplog.service.admin.adminUserService.impl;


import com.supplog.dto.admin.user.AdminUserResponseDto;
import com.supplog.dto.admin.user.AdminUserDetailResponseDto;
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
import com.supplog.repository.UserRepository;
import com.supplog.service.admin.adminExecutionService.AdminExecutionService;
import com.supplog.service.admin.adminRoutineService.AdminRoutineService;
import com.supplog.service.admin.adminSupplementService.AdminSupplementService;
import com.supplog.service.admin.adminSupportService.AdminSupportService;
import com.supplog.service.admin.adminUserService.AdminUserService;
import com.supplog.service.admin.audit.AdminAuditService;
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
    private final AdminSupplementService adminSupplementService;
    private final AdminRoutineService adminRoutineService;
    private final AdminExecutionService adminExecutionService;
    private final AdminSupportService adminSupportService;
    private final AdminAuditService adminAuditService;


    public AdminUserServiceImpl(UserRepository userRepository,
                                ModelMapper modelMapper,
                                PasswordEncoder passwordEncoder,
                                RoleRepository roleRepository,
                                TimeZoneResolver timeZoneResolver,
                                SupportService supportService,
                                AdminSupplementService adminSupplementService,
                                AdminRoutineService adminRoutineService,
                                AdminExecutionService adminExecutionService,
                                AdminSupportService adminSupportService,
                                AdminAuditService adminAuditService) {
        this.userRepository = userRepository;
        this.modelMapper = modelMapper;
        this.passwordEncoder = passwordEncoder;
        this.roleRepository = roleRepository;
        this.timeZoneResolver = timeZoneResolver;
        this.supportService = supportService;
        this.adminSupplementService = adminSupplementService;
        this.adminRoutineService = adminRoutineService;
        this.adminExecutionService = adminExecutionService;
        this.adminSupportService = adminSupportService;
        this.adminAuditService = adminAuditService;
    }


    @Override
    public AdminUserResponseDto getById(Long id) {


        User user = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("user.not.found", id));
        return toAdminUserResponseDto(user);
    }

    @Override
    public AdminUserDetailResponseDto getDetail(Long id) {
        return new AdminUserDetailResponseDto(
                getById(id),
                adminSupplementService.getAllSupplementsByUserId(id),
                adminRoutineService.getAllRoutinesByUserId(id),
                adminExecutionService.getRecentForUser(id),
                adminSupportService.search(null, id),
                adminAuditService.getRecentForResource("USER", id)
        );
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
    public List<AdminUserResponseDto> search(
            String username,
            String email,
            RoleName roleName,
            Boolean active
    ) {
        return userRepository.searchAdminUsers(
                        normalizeOptional(username),
                        normalizeOptional(email),
                        roleName,
                        active
                )
                .stream()
                .map(this::toAdminUserResponseDto)
                .toList();
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
    public void deactivateUser(Long currentAdminId, Long userId, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("user.not.found", userId));

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
            ensureAnotherActiveAdminExists("admin.last.active.cannot.deactivate");
        }

        supportService.handleUserDeactivation(userId);

        user.setTokenVersion(user.getTokenVersion() + 1);
        user.setDeleted(true);

        adminAuditService.record(
                currentAdminId,
                "USER_DEACTIVATED",
                "USER",
                userId,
                "active=true",
                "active=false",
                reason,
                true,
                null
        );
    }

    @Override
    @Transactional
    public void activateUser(Long currentAdminId, Long userId, String reason) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("user.not.found", userId));
        if (!user.isDeleted()) {
            throw new BusinessException("user.already.active");
        }
        user.setDeleted(false);

        adminAuditService.record(
                currentAdminId,
                "USER_ACTIVATED",
                "USER",
                userId,
                "active=false",
                "active=true",
                reason,
                true,
                null
        );
    }

    //Genişletilecek ve updateProileByAdmin için DTO oluşturulacak
    @Override
    @Transactional
    public void updateUserProfileByAdmin(Long currentAdminId, Long id, UpdateUserProfileRequestDtoByAdmin userProfileRequestDto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("user.not.found", id));

        String oldValue = userProfileSummary(user);
        String username = InputNormalizer.normalizeUsername(userProfileRequestDto.getUsername());

        String email = InputNormalizer.normalizeEmail(userProfileRequestDto.getEmail());

        userRepository.findByUsername(username)
                .filter(existingUser ->
                        !existingUser.getId().equals(id)
                )
                .ifPresent(existingUser -> {
                    throw new BusinessException(
                            "user.username.already.exists"
                    );
                });


        userRepository.findByEmail(email)
                .filter(existingUser ->
                        !existingUser.getId().equals(id)
                )
                .ifPresent(existingUser -> {
                    throw new BusinessException(
                            "user.email.already.exists"
                    );
                });


        user.setUsername(username);
        user.setEmail(email);

        user.setFirstName(InputNormalizer.trim(userProfileRequestDto.getFirstName()));
        user.setLastName(InputNormalizer.trim(userProfileRequestDto.getLastName()));
        user.setBirthDate(userProfileRequestDto.getBirthDate());

        adminAuditService.record(
                currentAdminId,
                "USER_PROFILE_UPDATED",
                "USER",
                id,
                oldValue,
                userProfileSummary(user),
                userProfileRequestDto.getReason(),
                true,
                null
        );
    }

    @Override
    @Transactional
    public void resetPassword(Long currentAdminId, Long id, ResetPasswordRequestDto request) {
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
        user.setMustChangePassword(true);

        adminAuditService.record(
                currentAdminId,
                "USER_PASSWORD_RESET",
                "USER",
                id,
                null,
                "temporaryPassword=true,mustChangePassword=true",
                request.getReason(),
                true,
                null
        );
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

        if (user.isDeleted() && requestedRoleName == RoleName.ROLE_ADMIN) {
            throw new BusinessException("admin.inactive.user.cannot.promote");
        }

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

        String oldRoles = user.getRoles().stream()
                .map(role -> role.getName().name())
                .sorted()
                .toList()
                .toString();

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
            ensureAnotherActiveAdminExists("admin.last.active.role.cannot.change");
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
        user.setTokenVersion(user.getTokenVersion() + 1);

        adminAuditService.record(
                currentAdminId,
                "USER_ROLE_UPDATED",
                "USER",
                id,
                oldRoles,
                requestedRoleName.name(),
                updateUserRoleRequestDto.getReason(),
                true,
                null
        );
    }

    private void ensureAnotherActiveAdminExists(String errorMessageKey) {
        List<User> activeAdmins =
                userRepository.findActiveUsersByRoleForUpdate(RoleName.ROLE_ADMIN);

        if (activeAdmins.size() <= 1) {
            throw new BusinessException(errorMessageKey);
        }
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

    private String normalizeOptional(String value) {
        String normalized = InputNormalizer.trim(value);
        return normalized == null || normalized.isBlank()
                ? null
                : normalized;
    }

    private String userProfileSummary(User user) {
        return "username=" + user.getUsername()
                + ",email=" + user.getEmail()
                + ",firstName=" + user.getFirstName()
                + ",lastName=" + user.getLastName()
                + ",birthDate=" + user.getBirthDate();
    }
}
