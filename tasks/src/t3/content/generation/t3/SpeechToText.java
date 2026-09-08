package t3.content.generation.t3;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import commons.Constants;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.UUID;

/**
 * T3-3: Speech to Text (Transcription)
 * <p>
 * Transcribes audio_sample.mp3 via /v1/audio/transcriptions using multipart/form-data.
 * Try both WHISPER_1 and GPT_4O_TRANSCRIBE models and compare the results.
 */
public class SpeechToText {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    public static void main(String[] args) throws Exception {
        //TODO:
        //  https://developers.openai.com/api/docs/guides/speech-to-text
        // - Load 'audio_sample.mp3' and define the model (e.g., 'whisper-1')
        // - Build a multipart/form-data request body manually (including boundary and fields)
        // - Send a POST request to Constants.OPENAI_AUDIO_TRANSCRIPTIONS_ENDPOINT
        // - Extract the 'text' from the response and print it

        // Define a unique boundary for the multipart data
        String boundary = "----JavaFormBoundary" + System.currentTimeMillis();

        // Create the multipart request body
        String requestBody = "--" + boundary + "\r\n"
            + "Content-Disposition: form-data; name=\"model\"\r\n\r\n"
            + Constants.GPT_4O_TRANSCRIBE + "\r\n"
            + "--" + boundary + "\r\n"
            + "Content-Disposition: form-data; name=\"file\"; filename=\"audio_sample.mp3\"\r\n"
            + "Content-Type: audio/mpeg\r\n\r\n";

        // Convert the initial StringBuilder to a byte array
        byte[] headerBytes = requestBody.getBytes();

        Path audioFilePath = Path.of("tasks/src/t3/content/generation/t3/audio_sample.mp3");
        byte[] audioBytes = Files.readAllBytes(audioFilePath);

        // Add the closing boundary
        String closingBoundary = "\r\n--" + boundary + "--\r\n";
        byte[] closingBoundaryBytes = closingBoundary.getBytes();

        // Compose the full binary request body
        byte[] finalBody = concatBytes(headerBytes, audioBytes, closingBoundaryBytes);
        var request = HttpRequest.newBuilder()
            .header("Content-Type", "multipart/form-data; boundary=" + boundary)
            .headers("Authorization", "Bearer " + System.getenv("OPENAI_API_KEY"))
            .uri(URI.create(Constants.OPENAI_AUDIO_TRANSCRIPTIONS_ENDPOINT))
            .POST(HttpRequest.BodyPublishers.ofByteArray(finalBody))
            .build();
        var response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        var json = MAPPER.readTree(response.body());
        System.out.println(json);

        String transcription = json.path("text").asText();
        String filename =
            "transcipt_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")) + ".txt";
        Path outputPath = Path.of("tasks/src/t3/content/generation/t3/" + filename);
        Files.writeString(outputPath, transcription);
    }

    // Utility method to concatenate byte arrays
    private static byte[] concatBytes(byte[]... arrays) {
        int totalLength = 0;
        for (byte[] array : arrays) {
            totalLength += array.length;
        }

        byte[] result = new byte[totalLength];
        int currentIndex = 0;

        for (byte[] array : arrays) {
            System.arraycopy(array, 0, result, currentIndex, array.length);
            currentIndex += array.length;
        }
        return result;
    }
}
