package com.project.BharatConnect.chatbot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class UploadFile {

    private final VectorStore vectorStore;

    public UploadFile(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public Boolean upload(List<String> message ) {

        List<Document> documents = message.stream()
                .map(Document::new)
                .toList();

        this.vectorStore.add(documents);
        return Boolean.TRUE;
    }
}
