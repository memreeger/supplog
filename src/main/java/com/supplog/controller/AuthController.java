package com.supplog.controller;


import com.supplog.dto.login.AuthResponseDto;
import com.supplog.dto.login.LoginRequestDto;
import com.supplog.dto.login.RegisterRequestDto;
import com.supplog.service.auth.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponseDto register(
            @Valid @RequestBody RegisterRequestDto request
    ) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponseDto login(
            @Valid @RequestBody LoginRequestDto request
    ) {
        return authService.login(request);
    }
}
