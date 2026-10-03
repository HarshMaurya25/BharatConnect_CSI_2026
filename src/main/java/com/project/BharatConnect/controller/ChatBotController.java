package com.project.BharatConnect.controller;

import com.project.BharatConnect.chatbot.ChatClientImplementation;
import com.project.BharatConnect.chatbot.ProfessionalTextChatBot;
import com.project.BharatConnect.chatbot.SummaryChatBot;
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
    private final SummaryChatBot summaryChatBot;
    private final ProfessionalTextChatBot professionalTextChatBot;

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

    @GetMapping("/summaries")
    public ResponseEntity<String> summary(
            @RequestParam String text
    ){
        return ResponseEntity.ok(summaryChatBot.answer(text));
    }

    @GetMapping("/rewrite")
    public ResponseEntity<String> rewrite(
            @RequestParam String text
    ){
        return ResponseEntity.ok(professionalTextChatBot.answer(text));
    }
}
