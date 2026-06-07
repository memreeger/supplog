package com.supplog.service.user;

import com.supplog.dto.user.CreateUserRequestDto;
import com.supplog.entity.User;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface UserService {
    User getById(long id);

    User getByUserName(String username);

    User getByEmail(String email);

    List<User> getAll();

    void addUser(CreateUserRequestDto userRequestDto);

    // change password

    // change ad soyad

}
