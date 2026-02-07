package controller;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class LLMPractice {
    public static void main(String[] args) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        String requestBody = """
            {
                "model": "gpt-3.5-turbo",
                "messages": [
                    {"role": "user", "content": "What is 2+2?"}
                ],
                "temperature": 0.7,
                "max_tokens": 100
            }
            """;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = client.send(request , HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            System.out.println("Success!");
            System.out.println(response.body());
        } else {
            System.err.println("Error: " + response.statusCode());
            System.err.println(response.body());
        }

    }
}
