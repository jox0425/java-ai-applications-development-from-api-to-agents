package t5.rag.advanced.embeddings;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class EmbeddingsClient {

    private final String endpoint;
    private final String apiKey;
    private final String modelName;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public EmbeddingsClient(String endpoint, String modelName, String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("API key cannot be null or empty");
        }
        this.endpoint = endpoint;
        this.apiKey = "Bearer " + apiKey;
        this.modelName = modelName;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Generate indexed embeddings for a single input string. Returns Map where key 0 holds the embedding vector.
     */
    public Map<Integer, List<Float>> getEmbeddings(String input, int dimensions) {
        return getEmbeddings(List.of(input), dimensions);
    }

    /**
     * Generate indexed embeddings for a list of input strings. Returns Map: inputs[0] -> [0][embedding], inputs[1] ->
     * [1][embedding], ...
     */
    public Map<Integer, List<Float>> getEmbeddings(List<String> inputs, int dimensions) {
        //TODO:
        // - https://developers.openai.com/api/reference/resources/embeddings/methods/create
        // - build JSON request body with "model", "input" (inputs list), "dimensions"
        // - set Authorization and Content-Type headers, POST to endpoint via httpClient
        // - on HTTP 200: parse response JSON, extract "data" array, return indexed embeddings via fromData()
        // - on non-200: throw RuntimeException with status code and response body
        // - wrap checked exceptions in RuntimeException
        try {
            var input = objectMapper.writeValueAsString(inputs);

            var bodyTemplate = """
                {
                    "input":%s,
                    "model":"%s",
                    "dimensions":%d
                }
                
                """;
            var body = String.format(bodyTemplate, input, modelName, dimensions);

            System.out.println(body);
            var request = HttpRequest.newBuilder(URI.create(endpoint))
                .header("Authorization", apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new RuntimeException(
                    String.format("Failed : HTTP error code : %s, response body: %s", response.statusCode(),
                        response.body()));
            } else {
                var responseJson = objectMapper.readTree(response.body());
                return fromData(responseJson.get("data"));
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<Integer, List<Float>> fromData(JsonNode data) {
        Map<Integer, List<Float>> result = new HashMap<>();
        for (JsonNode embeddingObj : data) {
            int index = embeddingObj.get("index").asInt();
            List<Float> embedding = new ArrayList<>();
            for (JsonNode value : embeddingObj.get("embedding")) {
                embedding.add((float) value.asDouble());
            }
            result.put(index, embedding);
        }
        return result;
    }
}
