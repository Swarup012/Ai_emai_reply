package com.email.writer.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
public class EmailGeneratorService {

    private final WebClient webClient;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    public EmailGeneratorService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public String generateEmailReply(EmailRequest emailRequest) {
        //Build the prompt
        String prompt = buildPrompt(emailRequest);
        //Craft the request
        Map<String,Object> requestBody = Map.of(
                "contents",new Object[]{
                        Map.of("parts",new Object[]{
                                Map.of("text",prompt)
                        })
                }
        );
        //Do req and get res
        String response = webClient.post()
                .uri(geminiApiUrl + geminiApiKey)
                .header("Content-Type","application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        //return res
        return extractResponse(response);
    }

    private String extractResponse(String response) {
        try{

            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(response);
            return  rootNode.path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();
        }catch (Exception e){
            return "Error processing response" + e.getMessage();
        }
    }

    private String buildPrompt(EmailRequest emailRequest) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an AI assistant that specializes in writing professional email replies. Your task is to generate a polished, well-structured, and contextually appropriate response based on the provided email content.  \n" +
                "\n" +
                "### **Response Guidelines:**  \n" +
                "1. **Direct and Professional:**  \n" +
                "   - Your response must **only** contain the email reply—no introductions, explanations, or extra commentary.  \n" +
                "   - Begin directly with a proper salutation (e.g., \"Dear [Sender Name],\").  \n" +
                "   - Do **not** generate a subject line.  \n" +
                "\n" +
                "2. **Clear and Courteous Communication:**  \n" +
                "   - Acknowledge the sender’s email appropriately.  \n" +
                "   - Provide a relevant and concise response addressing the sender’s concerns or requests.  \n" +
                "   - Maintain a professional yet polite tone throughout the reply.  \n" +
                "\n" +
                "3. **Structured Format:**  \n" +
                "   - Open with a greeting.  \n" +
                "   - Follow with a well-organized response addressing the key points.  \n" +
                "   - Close with a polite and professional sign-off (e.g., \"Best regards, [Your Name/Company Name]\").  \n" +
                "\n" +
                "4. **Use the Name ‘Swarup Basu’ in the Signature:** \n " +
                "### **Example Format:**  \n");
        if(emailRequest.getTone() != null && !emailRequest.getTone().isEmpty()) {
            prompt.append("Use a ").append(emailRequest.getTone()).append(" tone.");
        }
        prompt.append("\n Original email: \n").append(emailRequest.getEmailContent());
        return  prompt.toString();
    }
}

