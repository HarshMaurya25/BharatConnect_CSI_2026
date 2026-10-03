package com.project.BharatConnect.chatbot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ProfessionalTextChatBot {

    private final ChatClient chatClient;

    public ProfessionalTextChatBot(
            ChatClient.Builder builder,
            @Value("classpath:query/PostQuery.st") Resource summaryPrompt
    ) {

        this.chatClient = builder
                .defaultSystem(summaryPrompt)
                .build();
    }

    public String answer(String text) {
        return chatClient
                .prompt()
                .user(text)
                .call()
                .content();
    }
}

