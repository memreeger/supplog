package com.supplog.controller;


import com.supplog.dto.login.AuthResponseDto;
import com.supplog.dto.login.LoginRequestDto;
import com.supplog.dto.login.LoginResponseDto;
import com.supplog.dto.login.RegisterRequestDto;
import com.supplog.entity.User;
import com.supplog.repository.UserRepository;
import com.supplog.service.auth.AuthService;
import com.supplog.service.user.impl.CustomUserDetailsService;
import com.supplog.service.user.impl.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    /*
       private final JwtService jwtService;
       private final AuthenticationManager authenticationManager;
       private final PasswordEncoder passwordEncoder;
       private final UserDetailsService customUserDetailsService;
       private final UserRepository userRepository; // repodan alma servisten al


           public AuthController(JwtService jwtService, AuthenticationManager authenticationManager, PasswordEncoder passwordEncoder, CustomUserDetailsService customUserDetailsService, UserRepository userRepository) {
               this.jwtService = jwtService;
               this.authenticationManager = authenticationManager;
               this.passwordEncoder = passwordEncoder;
               this.customUserDetailsService = customUserDetailsService;
               this.userRepository = userRepository;
           }

           @PostMapping("/login")
           public LoginResponseDto login(@RequestBody LoginRequestDto requestDto){
               authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(requestDto.username(), requestDto.password()));

               String token = jwtService.generateToken(requestDto.username());

               return new LoginResponseDto(token);
           }

           @PostMapping("/register")
           public String register(@RequestBody RegisterRequestDto request) {

               // kullanıcı var mı kontrol
               if (userRepository.findByUsername(request.username()).isPresent()) {
                   return "Username already exists";
               }

               // yeni user oluştur
               User user = new User();
               user.setUsername(request.username());
               user.setPassword(passwordEncoder.encode(request.password()));

               userRepository.save(user);

               return "User registered successfully";
           }

        */
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public AuthResponseDto register(
            @RequestBody RegisterRequestDto request
    ) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponseDto login(
            @RequestBody LoginRequestDto request
    ) {
        return authService.login(request);
    }
}
