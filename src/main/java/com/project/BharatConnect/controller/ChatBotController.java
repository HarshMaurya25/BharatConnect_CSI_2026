package com.project.BharatConnect.controller;

import com.project.BharatConnect.chatbot.ChatClientImplementation;
import com.project.BharatConnect.chatbot.UploadFile;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/api/chatbot")
public class ChatBotController {
    private final ChatClientImplementation chatClientImplementation;
    private final UploadFile uploadFile;

    @PostMapping("/ask")
    public ResponseEntity<String> ask(
            @RequestParam String question
    ){
        return ResponseEntity.ok(chatClientImplementation.answer(question));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/info")
    public ResponseEntity<Boolean> sendData(
            @RequestBody List<String> data
            ){
        return ResponseEntity.ok(uploadFile.upload(data));
    }

}
