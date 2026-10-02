package com.project.BharatConnect.chatbot;

import com.project.BharatConnect.service.user.UserDetail;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ChatClientImplementation {

    private final ChatClient chatClient;

    public ChatClientImplementation(
            ChatClient.Builder builder,
            ChatMemoryRepository chatMemoryRepository,
            VectorStore vectorStore,
            @Value("classpath:query/SystemPromptSupport.st") Resource systemPrompt,
            @Value("classpath:query/RewriteQuery.st") Resource rewritePrompt
    ) {
        ChatMemory chatMemory = MessageWindowChatMemory
                .builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(6)
                .build();

        MessageChatMemoryAdvisor chatMemoryAdvisor = MessageChatMemoryAdvisor
                .builder(chatMemory)
                .order(1)
                .build();

        RetrievalAugmentationAdvisor.Builder retrieverAdvisor =
                RetrievalAugmentationAdvisor
                        .builder()
                        .queryTransformers(
                                query -> {
                                    String historyText = query.history()
                                            .stream()
                                            .filter(m -> m.getMessageType() == MessageType.USER)
                                            .map(m -> m.getMessageType() + ": " + m.getText())
                                            .collect(Collectors.joining("\n"));

                                    String enrichedText = historyText.isEmpty()
                                            ? query.text()
                                            : "Conversation History:"
                                            + historyText
                                            + "\nCurrent Query: " + query.text();

                                    return Query.builder()
                                            .text(enrichedText)
                                            .history(query.history())
                                            .context(query.context())
                                            .build();
                                },
                                RewriteQueryTransformer
                                        .builder()
                                        .chatClientBuilder(builder.clone())
                                        .targetSearchSystem("CollabIndia customer support")
                                        .promptTemplate(
                                                PromptTemplate
                                                        .builder()
                                                        .resource(rewritePrompt)
                                                        .build()
                                        )
                                        .build()
                        )
                        .documentRetriever(
                                VectorStoreDocumentRetriever
                                        .builder()
                                        .vectorStore(vectorStore)
                                        .similarityThreshold(0.55)
                                        .topK(3)
                                        .build()
                        )
                        .order(2);

        this.chatClient = builder
                .defaultAdvisors(
                        chatMemoryAdvisor,
                        retrieverAdvisor.build(),
                        new SimpleLoggerAdvisor()
                )
                .defaultSystem(systemPrompt)
                .build();
    }

    public String answer(String question) {
        UserDetail userDetail = (UserDetail) Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getPrincipal();

        assert userDetail != null;
        UUID userId = userDetail.getUser().getUserId();
        return chatClient
                .prompt()
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, userId.toString()))
                .user(question)
                .call()
                .content();
    }
}
