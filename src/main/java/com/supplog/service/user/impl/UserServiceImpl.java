package com.supplog.service.user.impl;

import com.supplog.dto.user.CreateUserRequestDto;
import com.supplog.entity.User;
import com.supplog.repository.UserRepository;
import com.supplog.service.user.UserService;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

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
    public User getById(long id) {
        return userRepository.getReferenceById(id);
    }

    @Override
    public User getByUserName(String userName) {
        return userRepository.findByUserName(userName).orElseThrow(() -> new IllegalArgumentException("Kullanıcı adı bulunamdı"));
    }

    @Override
    public User getByEmail(String email) {
        return null;
    }

    @Override
    public List<User> getAll() {
        return userRepository.findAll();
    }

    @Override
    public void addUser(CreateUserRequestDto userRequestDto) {
        User user = new User();
        mapper.map(userRequestDto, user);
        userRepository.save(user);
    }
}
