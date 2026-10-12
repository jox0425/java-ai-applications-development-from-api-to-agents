package t8.agent.task.tools;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import commons.Constants;

public class WebSearchTool extends BaseTool {

    private final String apiKey;
    private final String endpoint;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public WebSearchTool(String openAiApiKey) {
        this.apiKey = "Bearer " + openAiApiKey;
        this.endpoint = Constants.OPENAI_RESPONSES_ENDPOINT;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String getName() {
        //TODO: Return tool name "web_search_tool"
        return "web_search_tool";
    }

    @Override
    public String getDescription() {
        //TODO: Return a short description of what this tool does
        return "A tool that performs web searches using the OpenAI API.";
    }

    @Override
    public String getInputSchema() {
        //TODO: Return JSON schema for this tool — accepts a "request" string parameter (required)
        return """
            {
                    "type": "object",
                    "properties": {
                        "request": {
                            "type": "string",
                            "description": "The search query to perform."
                        }
                    },
                    "required": ["request"]
                }
            """;
    }

    @Override
    @SuppressWarnings("unchecked")
    public String execute(Map<String, Object> arguments) {
        // https://developers.openai.com/api/docs/guides/tools-web-search
        //TODO:
        // https://developers.openai.com/api/docs/guides/tools-web-search
        // - Build request payload: model "gpt-5.4", tools=[{"type":"web_search"}],
        //   input from arguments.get("request")
        // - POST to `endpoint` with Authorization header (`apiKey`) and Content-Type: application/json
        // - On HTTP 200: traverse output → find item with type "message"
        //   → find content block with type "output_text" → return its "text"
        // - Return error string on non-200 or exception (e.g. "Error: " + statusCode + " " + body)
        try {
            var requestPayload = String.format("""
                {
                    "model": "gpt-5.4",
                    "tools": [{"type": "web_search"}],
                    "input": "%s"
                }
                """, arguments.get("request"));
            var request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(requestPayload))
                .uri(URI.create(endpoint))
                .header("Authorization", apiKey)
                .header("Content-Type", "application/json")
                .build();
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return extractOutputText(response.body());
            } else {
                return "Error: " + response.statusCode() + " " + response.body();
            }
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    private String extractOutputText(String root) throws JsonProcessingException {
        var rootNode = objectMapper.readTree(root);
        for (JsonNode item : rootNode.path("output")) {
            if ("message".equals(item.path("type").asText())) {
                for (JsonNode contentPart : item.path("content")) {
                    if ("output_text".equals(contentPart.path("type").asText())) {
                        return contentPart.path("text").asText("");
                    }
                }
            }
        }
        throw new RuntimeException("No output text found in the response");
    }
}
