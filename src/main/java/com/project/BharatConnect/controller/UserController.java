package com.project.BharatConnect.controller;

import com.project.BharatConnect.service.user.UserService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/test/admin")
    public ResponseEntity<String> forAdmin(){
        return ResponseEntity.ok("Admin");
    }

    @GetMapping("/test/user")
    public ResponseEntity<String> forUser(){
        return ResponseEntity.ok("Admin");
    }

    @PreAuthorize("#userId.toString() == authentication.principal.toString() or hasRole('ADMIN')")
    public ResponseEntity<String> forUserId(
            @RequestParam String id
    ){
        return ResponseEntity.ok("Admin");
    }

}
