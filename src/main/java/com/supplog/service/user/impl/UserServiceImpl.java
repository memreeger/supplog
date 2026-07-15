package com.supplog.service.user.impl;

import com.supplog.dto.user.*;
import com.supplog.entity.User;
import com.supplog.exception.BusinessException;
import com.supplog.exception.ResourceNotFoundException;
import com.supplog.repository.RoutineRepository;
import com.supplog.repository.SupplementRepository;
import com.supplog.repository.UserRepository;
import com.supplog.service.user.UserService;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
//@Primary
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;
    private final SupplementRepository supplementRepository;
    private final RoutineRepository routineRepository;

    public UserServiceImpl(UserRepository userRepository, ModelMapper modelMapper, PasswordEncoder passwordEncoder, SupplementRepository supplementRepository, RoutineRepository routineRepository) {
        this.userRepository = userRepository;
        this.modelMapper = modelMapper;
        this.passwordEncoder = passwordEncoder;
        this.supplementRepository = supplementRepository;
        this.routineRepository = routineRepository;
    }


    @Override
    public UserResponseDto getMyProfile(String username) {
        User user = findUserByUsername(username);
        return modelMapper.map(user, UserResponseDto.class);
    }

    @Override
    public UserResponseDto getMyProfile(Long id) {
        User user = findActiveUserById(id);
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

        user.setTokenVersion(user.getTokenVersion() + 1);
        user.setPassword(passwordEncoder.encode(changePasswordRequestDto.getNewPassword()));
        userRepository.save(user);

    }

    @Override
    public void changeMyPassword(Long id, ChangePasswordRequestDto changePasswordRequestDto) {
        User user = findActiveUserById(id);
        if (!passwordEncoder.matches(changePasswordRequestDto.getOldPassword(), user.getPassword())) {
            throw new BusinessException("user.old.password.incorrect");
        }

        if (!changePasswordRequestDto.getNewPassword().equals(changePasswordRequestDto.getConfirmPassword())) {
            throw new BusinessException("user.password.not.match");
        }

        user.setTokenVersion(user.getTokenVersion() + 1);
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
    public void updateMyProfile(Long id, UpdateUserProfileRequestDto userProfileRequestDto) {
        User user = findActiveUserById(id);
        user.setFirstName(userProfileRequestDto.getFirstName());
        user.setLastName(userProfileRequestDto.getLastName());
        userRepository.save(user);

    }

    @Override
    @Transactional
    public void deActivateMyProfile(String username, DeleteUserRequestDto deleteUserRequestDto) {
        User user = findUserByUsername(username);

        if (!passwordEncoder.matches(deleteUserRequestDto.getPassword(), user.getPassword())) {
            throw new BusinessException("user.password.not.match");
        }

        routineRepository.softDeleteAllByUserId(user.getId());
        supplementRepository.softDeleteAllByUserId(user.getId());
        user.setDeleted(true);
        userRepository.save(user);

    }

    @Override
    @Transactional
    public void deActivateMyProfile(Long id, DeleteUserRequestDto deleteUserRequestDto) {
        User user = findActiveUserById(id);

        if (!passwordEncoder.matches(deleteUserRequestDto.getPassword(), user.getPassword())) {
            throw new BusinessException("user.password.not.match");
        }

        routineRepository.softDeleteAllByUserId(id);
        supplementRepository.softDeleteAllByUserId(id);
        user.setDeleted(true);
        userRepository.save(user);

    }


    //HELPER
    private User findUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("user.not.found"));
    }

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

    /*
    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        User user = userRepository
                .findByUsernameAndIsDeletedFalse(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "Active user not found: " + username
                        )
                );

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(user.getRoles().stream().map(role-> new SimpleGrantedAuthority(role.getName().name())).toList())
                .build();
    }

     */
}
