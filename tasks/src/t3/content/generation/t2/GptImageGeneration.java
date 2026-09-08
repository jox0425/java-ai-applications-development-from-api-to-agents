package t3.content.generation.t2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import commons.Constants;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * T3-2b: GPT-Image-1 Image Generation
 * <p>
 * Generates an image via /v1/images/generations using gpt-image-1.
 * Unlike DALL-E 3, the response returns the image as base64 JSON (b64_json)
 * rather than a URL — this implementation decodes and saves it as a PNG file.
 */
public class GptImageGeneration {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    public static void main(String[] args) throws Exception {
        //TODO:
        // - Define a JSON payload for /v1/images/generations with model 'gpt-image-2'
        // - Set 'prompt' to 'Smiling catdog'
        // - Send a POST request to Constants.OPENAI_IMAGES_GENERATIONS_ENDPOINT
        // - Extract 'b64_json' from the response, decode it, and save as a .png file

        var jsonTemplate = """
                {
                    "model": "gpt-image-2",
                    "prompt": "Smiling catdog"
                }
            """;

        var request = HttpRequest.newBuilder()
            .header("Content-Type", "application/json")
            .headers("Authorization", "Bearer " + System.getenv("OPENAI_API_KEY"))
            .uri(URI.create(Constants.OPENAI_IMAGES_GENERATIONS_ENDPOINT))
            .POST(HttpRequest.BodyPublishers.ofString(jsonTemplate))
            .build();
        var response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        var json = MAPPER.readTree(response.body());
        System.out.println(json);

        var imageInB64 = json.path("data").path(0).path("b64_json").asText();
        Base64.Decoder decoder = Base64.getDecoder();
        var imgBytes = decoder.decode(imageInB64);
        String filename =
            "generated_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")) + ".png";
        Path outputPath = Path.of("tasks/src/t3/content/generation/t2/" + filename);
        Files.write(outputPath, imgBytes);

    }
}

//  https://developers.openai.com/api/reference/resources/images/methods/generate
//  ---
//  Request:
//  curl -X POST "https://api.openai.com/v1/images/generations" \
//      -H "Authorization: Bearer $OPENAI_API_KEY" \
//      -H "Content-type: application/json" \
//      -d '{
//          "model": "gpt-image-2",
//          "prompt": "smiling catdog."
//      }'
//  Response:
//  {
//    "created": 1699900000,
//    "data": [
//      {
//        "b64_json": Qt0n6ArYAEABGOhEoYgVAJFdt8jM79uW2DO...,
//      }
//    ]
//  }
