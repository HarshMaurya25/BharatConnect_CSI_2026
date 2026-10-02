package com.project.BharatConnect.controller;

import com.project.BharatConnect.entity.User;
import com.project.BharatConnect.service.user.UserDetail;
import com.project.BharatConnect.service.user.UserService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;



@RestController
@AllArgsConstructor
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

//    @GetMapping("/test")
//    @PreAuthorize("#id == authentication.principal.user.userId or hasRole('ADMIN')")
//    public ResponseEntity<String> forUserId(
//            @RequestParam UUID id
//    ) {
//        return ResponseEntity.ok("User allowed");
//    }
//
//    @GetMapping("/test/id")
//    public ResponseEntity<String> test(
//            @RequestParam UUID id,
//            Authentication authentication
//    ) {
//        System.out.println("Request ID: " + id);
//        System.out.println("Principal: " + authentication.getPrincipal());
//        System.out.println("Principal class: " +
//                authentication.getPrincipal().getClass());
//
//        UserDetail userDetail =
//                (UserDetail) authentication.getPrincipal();
//
//        UUID userId = userDetail.getUser().getUserId();
//
//        System.out.println("User ID: " + userId);
//        System.out.println("Authorities: " + authentication.getAuthorities());
//
//        return ResponseEntity.ok("test");
//    }

}
