package com.example.SuViet.controller;

import com.example.SuViet.config.CustomUserDetailService;
import com.example.SuViet.model.Role;
import com.example.SuViet.response.ResponseJwt;
import com.example.SuViet.model.User;

import com.example.SuViet.dto.LoginDTO;
import com.example.SuViet.dto.SignUp;
import com.example.SuViet.repository.RoleRepository;
import com.example.SuViet.repository.UserRepository;
import com.example.SuViet.service.ImageStorageService;
import com.example.SuViet.service.JwtService;
import com.example.SuViet.service.UserService;
import com.example.SuViet.utils.Utility;
import io.jsonwebtoken.impl.DefaultClaims;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000")
public class UserController {
    @Autowired
    private JwtService jwtService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private CustomUserDetailService userDetailService;

    @Autowired
    private ImageStorageService imageStorageService;

    @PostMapping("/login")
    public ResponseEntity<ResponseJwt> login(@RequestBody LoginDTO loginDTO) {
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginDTO.getMail(), loginDTO.getPassword()));
        if (authentication.isAuthenticated()) {
            boolean isEnabled = userRepository.findByMail(loginDTO.getMail()).get().isEnabled();
            if (!isEnabled) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                        new ResponseJwt("FAILED", "FAILED", -1, "", "", "", "","", "")
                );
            }
            String roleName = "";
            List<Role> roles = (List<Role>) userRepository.findByMail(loginDTO.getMail()).get().getRoles();
            for (Role role : roles) {
                roleName = role.getRoleName();
            }

            UserDetails userdetails = userDetailService.loadUserByUsername(loginDTO.getMail());
            String token = jwtService.generateToken(userdetails);
            int userID = userRepository.findByMail(loginDTO.getMail()).get().getUserID();
            String avatar = userRepository.findByMail(loginDTO.getMail()).get().getAvatar();
            return ResponseEntity.ok(
                    new ResponseJwt("OK", "Login successfully", userID, loginDTO.getMail(), loginDTO.getPassword(),
                            userRepository.findByMail(loginDTO.getMail()).get().getFullname(),
                            "http://localhost:8080/api/user/files/" + avatar, roleName, token));
        } else {
            throw new UsernameNotFoundException("invalid user request !");
        }

    }

    @GetMapping("/files/{filename:.+}")
    public ResponseEntity<byte[]> readDetailFile(@PathVariable String filename) {
        try {
            byte[] bytes = imageStorageService.readFileContent(filename, "avatar");
            return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.IMAGE_JPEG)
                    .body(bytes);
        } catch (Exception e) {
            return ResponseEntity.noContent().build();
        }
    }

    @GetMapping("/checkEmail")
    public ResponseEntity<String> getEmail(@AuthenticationPrincipal OAuth2User oauth2User) {
        if (oauth2User == null) {
            return ResponseEntity.ok(new String("Please login!"));
        }
        String email = oauth2User.getAttribute("email");
        User user = userRepository.findByMailAndEnabled(email, true).get();
        if (user != null) {
            return ResponseEntity.ok(new String("Email has been used to sign up!"));
        } else {
            return ResponseEntity.ok(new String("Login successfully!"));
        }
    }


    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignUp signUp, HttpServletRequest request)
            throws MessagingException, UnsupportedEncodingException {
        if (userRepository.existsByMail(signUp.getMail())) {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body("User has already exist!!!");
        }

        userService.registerANewMember(signUp);
        User user = userRepository.findByMail(signUp.getMail()).get();
        String siteURL = Utility.getSiteUrl(request);
        userService.sendVerificationMailToRegistration(user, siteURL);
        return ResponseEntity.status(HttpStatus.OK).body(
                "Sign up succcessfully!!!" +
                        "Please check your email to verify your account.");
    }


    @GetMapping("/verify")
    public ResponseEntity<String> verifyAccount(@Param("code") String code) {
        boolean verified = userService.verify(code);
        if (verified) {
            return ResponseEntity.ok("Verify successfully!");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Verify failed!");
        }
    }

    @PostMapping("/forgot")
    public String forgotPassword(@RequestParam(value = "mail") String mail, HttpServletRequest request) throws MessagingException, UnsupportedEncodingException {
        return userService.checkMailStatus(mail, request);
    }

    @PostMapping("/reset-password/{code}")
    public String resetPassword(@RequestParam("password") String password, @PathVariable String code) {
        boolean resetPassword = userService.resetPassword(password, code);
        if (!resetPassword) {
            return "User does not exist!";
        } else {
            return "Reset password successfully";
        }
    }

    @GetMapping("/refresh-token")
    public String refreshToken(HttpServletRequest request) throws Exception {
        DefaultClaims claims = (io.jsonwebtoken.impl.DefaultClaims) request.getAttribute("claims");

        Map<String, Object> expectedMap = getMapFromIoJsonwebtokenClaims(claims);
        String token = jwtService.doGenerateRefreshToken(expectedMap, expectedMap.get("sub").toString());
        return token;
    }

    public Map<String, Object> getMapFromIoJsonwebtokenClaims(DefaultClaims claims) {
        Map<String, Object> expectedMap = new HashMap<String, Object>();
        for (Map.Entry<String, Object> entry : claims.entrySet()) {
            expectedMap.put(entry.getKey(), entry.getValue());
        }
        return expectedMap;
    }
}
