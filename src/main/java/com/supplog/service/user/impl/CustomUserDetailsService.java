package com.supplog.service.user.impl;

import com.supplog.entity.User;
import com.supplog.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }


    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        User user = userRepository
                .findByUsernameAndIsDeletedFalse(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "Active user not found: " + username
                        )
                );

        return new CustomUserDetails(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                !user.isDeleted(),
                user.getEmail(),
                user.getTokenVersion(),
                user.getRoles()
                        .stream()
                        .map(role ->
                                new SimpleGrantedAuthority(
                                        role.getName().name()
                                )
                        )
                        .toList()
        );
    }
}
