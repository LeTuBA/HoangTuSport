package com.lat.be.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import com.lat.be.domain.Role;
import com.lat.be.domain.User;
import com.lat.be.domain.request.ReqLoginDTO;
import com.lat.be.domain.response.ResCreateUserDTO;
import com.lat.be.domain.response.ResLoginDTO;
import com.lat.be.service.RoleService;
import com.lat.be.service.UserService;
import com.lat.be.util.SecurityUtil;
import com.lat.be.util.annotation.ApiMessage;
import com.lat.be.util.error.IdInvalidException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class AuthController {

    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final SecurityUtil securityUtil;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final RoleService roleService;

    @Value("${lat.jwt.refresh-token-validity-in-seconds}")
    private long refreshTokenExpiration;

    @PostMapping("/auth/login")
    public ResponseEntity<ResLoginDTO> login(@Valid @RequestBody ReqLoginDTO loginDTO) {
        //Nạp input gồm username/password vào Security
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                loginDTO.getUsername(), loginDTO.getPassword());
        //xác thực người dùng => cần viết hàm loadUserByUsername
        Authentication authentication =
                authenticationManagerBuilder.getObject().authenticate(authenticationToken);
        // create a token
        SecurityContextHolder.getContext().setAuthentication(authentication);
        ResLoginDTO res = new ResLoginDTO();

        User currentUserDB = this.userService.handleGetUserByEmail(loginDTO.getUsername());
        if(currentUserDB != null){
            ResLoginDTO.UserLogin userLogin = new ResLoginDTO.UserLogin(
                    currentUserDB.getId(),
                    currentUserDB.getEmail(),
                    currentUserDB.getName(),
                    currentUserDB.getRole(),
                    currentUserDB.getAvatar(),
                    currentUserDB.getAddress(),
                    currentUserDB.getGender()
            );
            res.setUser(userLogin);
        }
        String access_token = this.securityUtil.createAccessToken(authentication.getName(), res);


        res.setAccessToken(access_token);

        // create a refresh token
        String refresh_token = this.securityUtil.createRefreshToken(loginDTO.getUsername(), res);
        //update user
        this.userService.updateUserToken(refresh_token, loginDTO.getUsername());

        //set cookies
        ResponseCookie resCookies = ResponseCookie.from("refresh_token", refresh_token)
                .httpOnly(true)
                .maxAge(refreshTokenExpiration)
                .path("/")
                .build();

        res.setRefreshToken(refresh_token);


        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, resCookies.toString())
                .body(res);


    }


    @GetMapping("/auth/account")
    @ApiMessage("fetch account")
    public ResponseEntity<ResLoginDTO.UserGetAccount> getAccount(){
        String currentEmail = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng hiện tại"));
        
        User currentUser = userService.handleGetUserByEmail(currentEmail);
        ResLoginDTO.UserLogin userLogin = new ResLoginDTO.UserLogin();
        ResLoginDTO.UserGetAccount userGetAccount = new ResLoginDTO.UserGetAccount(userLogin);
        if(currentUser != null){
            userLogin.setId(currentUser.getId());
            userLogin.setEmail(currentUser.getEmail());
            userLogin.setName(currentUser.getName());
            userLogin.setAvatar(currentUser.getAvatar());
            userLogin.setAddress(currentUser.getAddress());
            userLogin.setGender(currentUser.getGender());
            userLogin.setRole(currentUser.getRole());
            userGetAccount.setUser(userLogin);
        }
        return ResponseEntity.ok(userGetAccount);
    }

    @GetMapping("/auth/refresh")
    @ApiMessage("Get user by refresh token")
    public ResponseEntity<ResLoginDTO> getRefreshToken(@CookieValue(name = "refresh_token", defaultValue = "default") String refreshToken) throws IdInvalidException {
        if(refreshToken.equals("default")){
            throw new IdInvalidException("Bạn không có Refresh token Cookie");
        }

        // check valid
        Jwt decodedToken =  this.securityUtil.checkValidRefreshToken(refreshToken);
        String email = decodedToken.getSubject();

        // check user by token and email
        User currentUser = this.userService.getUserByRefreshTokenAndEmail(refreshToken, email);
        if(currentUser == null){
            throw new IdInvalidException("Refresh token không hợp lệ");
        }



        //issue new token/set refresh token as cookies
        ResLoginDTO res = new ResLoginDTO();

        User currentUserDB = this.userService.handleGetUserByEmail(email);
        if(currentUserDB != null){
            ResLoginDTO.UserLogin userLogin = new ResLoginDTO.UserLogin(
                    currentUserDB.getId(),
                    currentUserDB.getEmail(),
                    currentUserDB.getName(),
                    currentUserDB.getRole(),
                    currentUserDB.getAvatar(),
                    currentUserDB.getAddress(),
                    currentUserDB.getGender()
            );
            res.setUser(userLogin);
        }
        String access_token = this.securityUtil.createAccessToken(email, res);


        res.setAccessToken(access_token);

        // create a refresh token
        String new_refresh_token = this.securityUtil.createRefreshToken(email, res);
        //update user
        this.userService.updateUserToken(new_refresh_token, email);

        //set cookies
        ResponseCookie resCookies = ResponseCookie.from("refresh_token", new_refresh_token)
                .httpOnly(true)
                .maxAge(refreshTokenExpiration)
                .path("/")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, resCookies.toString())
                .body(res);

    }

    @PostMapping("/auth/logout")
    @ApiMessage("Logout")
    public ResponseEntity<Void> logout() throws IdInvalidException {
        String email = SecurityUtil.getCurrentUserLogin().isPresent() ?
                SecurityUtil.getCurrentUserLogin().get() : "";
        if(email.equals("")){
            throw new IdInvalidException("Access token không hợp lệ");
        }

        //update refresh token = null
        this.userService.updateUserToken(null, email);

        //remove refresh cookies
        ResponseCookie deleteSpringCookie = ResponseCookie.from("refresh_token", null)
                .httpOnly(true)
                .secure(true)
                .maxAge(0)
                .path("/")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, deleteSpringCookie.toString())
                .build();
    }

    @PostMapping("/auth/register")
    @ApiMessage("Register a new user")
    public ResponseEntity<ResCreateUserDTO> register(@Valid @RequestBody User postmanUser) throws IdInvalidException {
        boolean isEmailExist = this.userService.existsByEmail(postmanUser.getEmail());
        if(isEmailExist){
            throw new IdInvalidException("Email " + postmanUser.getEmail() +" đã tồn tại, vui lòng chọn email khác");
        }

        String hashPassword = this.passwordEncoder.encode(postmanUser.getPassword());
        postmanUser.setPassword(hashPassword);
        Role role = this.roleService.fetchRoleByName("user");
        postmanUser.setRole(role);
        User newUser = this.userService.handleCreateUser(postmanUser, null);
        return ResponseEntity.status(HttpStatus.CREATED).body(this.userService.convertToResCreateUserDTO(newUser));
    }
}
