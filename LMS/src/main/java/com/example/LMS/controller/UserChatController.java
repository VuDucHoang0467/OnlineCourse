package com.example.LMS.controller;


import com.example.LMS.model.User;
import com.example.LMS.repository.UserRepository;
import com.example.LMS.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserChatController {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @PostMapping("/registerchat")
    public ResponseEntity<?> registerUser(@RequestBody User user) {
        // Mã hóa mật khẩu và lưu người dùng vào cơ sở dữ liệu
        user.setPassword(new BCryptPasswordEncoder().encode(user.getPassword()));
        userRepository.save(user);
        return ResponseEntity.ok("User registered successfully");
    }

    @PostMapping("/loginchat")
    public ResponseEntity<?> loginUser(@RequestBody User user) {
        User foundUser = userRepository.findByUsername(user.getUsername());
        // Kiểm tra mật khẩu
        if (foundUser != null && new BCryptPasswordEncoder().matches(user.getPassword(), foundUser.getPassword())) {
            // Trả về thông tin người dùng
            Map<String, Object> userData = new HashMap<>();
            userData.put("name", foundUser.getName());
            userData.put("username", foundUser.getUsername());
            userData.put("email", foundUser.getEmail());
            userData.put("phone", foundUser.getPhoneNumber());
            userData.put("address", foundUser.getAddress());
            return ResponseEntity.ok(userData);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
        }
    }

    @GetMapping("/user/{id}")
    public ResponseEntity<String> viewUserDetails(@PathVariable("id") Long id, Model model) {
        User user = userService.findById(id);
        model.addAttribute("user", user);
        return ResponseEntity.ok("User info");
    }
}
