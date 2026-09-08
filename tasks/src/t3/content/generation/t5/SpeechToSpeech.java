package t3.content.generation.t5;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

import com.fasterxml.jackson.databind.ObjectMapper;
import commons.Constants;

/**
 * T3-5: Speech to Speech
 * <p>
 * Sends an audio question (question.mp3) as a base64-encoded input_audio message to gpt-4o-audio-preview via
 * /v1/chat/completions with modalities=["text","audio"]. The model responds with both text and audio; the audio is
 * decoded from base64 and saved as an MP3 file.
 */
public class SpeechToSpeech {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    public static void main(String[] args) throws Exception {
        //TODO:
        //  https://developers.openai.com/api/docs/guides/audio#add-audio-to-your-existing-application
        // - Load 'question.mp3' and encode it to base64
        // - Define a JSON payload for /v1/chat/completions with model 'gpt-4o-audio-preview'
        // - Set 'modalities' to ["text", "audio"] and configure the 'audio' object (voice, format)
        // - Send a POST request to Constants.OPENAI_CHAT_COMPLETIONS_ENDPOINT
        // - Extract 'audio.data' from the response, decode it, and save as an .mp3 file

        var jsonTemplate = """
            {
                  "model": "gpt-audio-1.5",
                  "modalities": ["text", "audio"],
                  "audio": { "voice": "alloy", "format": "wav" },
                  "messages": [
                    {
                      "role": "user",
                      "content": [
                        { "type": "text", "text": "What is in this recording?" },
                        {
                          "type": "input_audio",
                          "input_audio": {
                            "data": "%s",
                            "format": "mp3"
                          }
                        }
                      ]
                    }
                  ]
                }
            """;
        Path audioFilePath = Path.of("tasks/src/t3/content/generation/t5/question.mp3");
        byte[] audioBytes = Files.readAllBytes(audioFilePath);
        var audioBase64 = Base64.getEncoder().encodeToString(audioBytes);
        var jsonBody = String.format(jsonTemplate, audioBase64);

        var request = HttpRequest.newBuilder()
            .header("Content-Type", "application/json")
            .headers("Authorization", "Bearer " + System.getenv("OPENAI_API_KEY"))
            .uri(URI.create(Constants.OPENAI_CHAT_COMPLETIONS_ENDPOINT))
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();

        var response = HTTP.send(request, HttpResponse.BodyHandlers.ofByteArray());
        var json = MAPPER.readTree(response.body());
        System.out.println(json);
        var audioB64 = json.path("choices").path(0).path("message").path("audio").path("data").asText();
        var decodedAudio = Base64.getDecoder().decode(audioB64);
        var filename = "sts_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")) + ".mp3";
        Path outputPath = Path.of("tasks/src/t3/content/generation/t5/").resolve(filename);
        Files.write(outputPath, decodedAudio);
    }

}
