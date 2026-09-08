package t3.content.generation.t2;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import commons.Constants;

/**
 * T3-2c: GPT-Image-1 Image Edit
 * <p>
 * Edits an existing image via /v1/images/edits using gpt-image-1. Request is multipart/form-data (NOT JSON) and
 * includes the original image, the model name, and an edit prompt. The response returns the edited image as base64 JSON
 * (b64_json) which is decoded and saved as a PNG file.
 */
public class GptImageEdit {

    private static final String OPENAI_IMAGES_EDITS_ENDPOINT = "https://api.openai.com/v1/images/edits";

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    public static void main(String[] args) throws Exception {
        //TODO:
        //  https://platform.openai.com/docs/api-reference/images/createEdit
        // - Load 'logo.png' and define the model (e.g., 'gpt-image-1') and an edit prompt
        // - Build a multipart/form-data request body manually (including boundary and fields: model, prompt, image)
        // - Send a POST request to OPENAI_IMAGES_EDITS_ENDPOINT
        // - Extract the 'b64_json' from data[0], decode base64, and save the edited image as PNG

        Path logoPath = Path.of("tasks/src/t3/content/generation/t2/logo.png");
        byte[] logoBytes = Files.readAllBytes(logoPath);
        var model = "gpt-image-1";
        var prompt = "Transform the attached image to greyscale. Preserve size.";
        String logoBase64 = "data:image/png;base64," + Base64.getEncoder().encodeToString(logoBytes);
        // Define a unique boundary for the multipart data
        String boundary = "Boundary-" + UUID.randomUUID();

        // Create the multipart request body
        StringBuilder requestBody = new StringBuilder();

        // Add model field
        requestBody.append("--").append(boundary).append("\r\n")
            .append("Content-Disposition: form-data; name=\"model\"\r\n\r\n")
            .append(model).append("\r\n");

        // Add prompt field
        requestBody.append("--").append(boundary).append("\r\n")
            .append("Content-Disposition: form-data; name=\"prompt\"\r\n\r\n")
            .append(prompt).append("\r\n");

        // Add binary image file (header part only, binary data is appended later)
        requestBody.append("--").append(boundary).append("\r\n")
            .append("Content-Disposition: form-data; name=\"image\"; filename=\"logo.png\"\r\n")
            .append("Content-Type: image/png\r\n\r\n");

        // Convert the initial StringBuilder to a byte array
        byte[] headerBytes = requestBody.toString().getBytes();

        // Add the closing boundary
        String closingBoundary = "\r\n--" + boundary + "--\r\n";
        byte[] closingBoundaryBytes = closingBoundary.getBytes();

        // Compose the full binary request body
        byte[] finalBody = concatBytes(headerBytes, logoBytes, closingBoundaryBytes);

        var request = HttpRequest.newBuilder()
            .header("Content-Type", "multipart/form-data; boundary=" + boundary)
            .headers("Authorization", "Bearer " + System.getenv("OPENAI_API_KEY"))
            .uri(URI.create(Constants.OPENAI_IMAGES_EDIT_ENDPOINT))
            .POST(HttpRequest.BodyPublishers.ofByteArray(finalBody))
            .build();
        var response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        var json = MAPPER.readTree(response.body());
        System.out.println(json);
        String imageBase64 = json.path("data").path(0).path("b64_json").asText();
        byte[] editedBytes = Base64.getDecoder().decode(imageBase64);
        String filename =
            "edited_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")) + ".png";
        Path outputPath = Path.of("tasks/src/t3/content/generation/t2/" + filename);
        Files.write(outputPath, editedBytes);
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

// https://developers.openai.com/api/reference/resources/images/methods/edit
// ---
// Request (multipart/form-data, NOT json):
// curl -X POST "https://api.openai.com/v1/images/edits" \
//     -H "Authorization: Bearer $OPENAI_API_KEY" \
//     -F "model=gpt-image-1" \
//     -F "image=@logo.png" \
//     -F "prompt=Add magical sparkles and glowing aura around the logo"
// Response:
// {
//   "created": 1699900000,
//   "data": [
//     {
//       "b64_json": "Qt0n6ArYAEABGOhEoYgVAJFdt8jM79uW2DO..."
//     }
//   ]
// }