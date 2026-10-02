package com.project.BharatConnect;

import org.springframework.ai.model.google.genai.autoconfigure.chat.GoogleGenAiChatAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@ConfigurationPropertiesScan
@SpringBootApplication(exclude = {
		GoogleGenAiChatAutoConfiguration.class,
		OpenAiEmbeddingAutoConfiguration.class
})
public class BharatConnectApplication {

	public static void main(String[] args) {
		SpringApplication.run(BharatConnectApplication.class, args);
	}

}
