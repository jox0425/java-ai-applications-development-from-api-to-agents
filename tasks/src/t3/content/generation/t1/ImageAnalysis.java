package t3.content.generation.t1;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import com.fasterxml.jackson.databind.ObjectMapper;
import commons.Constants;

/**
 * T3-1: Image Analysis (Vision)
 * <p>
 * Sends two images to gpt model via /v1/chat/completions: - a remote image by URL - a local logo.png encoded as a
 * base64 data URL and asks the model to write a poem based on both images.
 */
public class ImageAnalysis {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    public static void main(String[] args) throws Exception {
        //TODO:
        //  https://developers.openai.com/api/docs/guides/images-vision?format=url&lang=curl
        //  https://developers.openai.com/api/docs/guides/images-vision?format=base64-encoded
        // - Encode local 'logo.png' to a base64 data URL
        // - Define a JSON payload for /v1/chat/completions with model 'gpt-5.4' (or some other OpenAI LLM)
        // - Include a user message with 3 content parts: prompt text, remote image URL, and local base64 image
        // - Send a POST request to Constants.OPENAI_CHAT_COMPLETIONS_ENDPOINT with Bearer token
        // - Extract and print the poem from the response JSON

        Path logoPath = Path.of("tasks/src/t3/content/generation/t1/logo.png");
        byte[] logoBytes = Files.readAllBytes(logoPath);
        String dataUrl = "data:image/png;base64," + Base64.getEncoder().encodeToString(logoBytes);
        String remoteUrl = "https://a-z-animals.com/media/2019/11/Elephant-male-1024x535.jpg";
        var jsonTemplate = """
                {
                    "model": "gpt-5.4",
                    "messages": [
                      {
                        "role": "user",
                        "content": [
                          {
                            "type": "text",
                            "text": "Write a funny and short poem about the two images attached"
                          },
                          {
                            "type": "image_url",
                            "image_url": {
                              "url": "%s"
                            }
                          },
                          {
                            "type": "image_url",
                            "image_url": {
                              "url": "%s"
                            }
                          }
                        ]
                      }
                    ],
                    "max_completion_tokens": 300
                }
            """;
        String jsonString = String.format(jsonTemplate, remoteUrl, dataUrl);

        var request = HttpRequest.newBuilder()
            .header("Content-Type", "application/json")
            .headers("Authorization", "Bearer " + System.getenv("OPENAI_API_KEY"))
            .uri(URI.create(Constants.OPENAI_CHAT_COMPLETIONS_ENDPOINT))
            .POST(HttpRequest.BodyPublishers.ofString(jsonString))
            .build();
        var response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        var json = MAPPER.readTree(response.body());
        // System.out.println(json);
        var poem = json.path("choices").path(0).path("message").path("content").asText();
        System.out.println(poem);
    }
}
