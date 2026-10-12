package t8.agent.task.agents;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import commons.Constants;
import commons.exceptions.TaskNotImplementedException;
import commons.model.Message;
import commons.model.Role;
import t8.agent.task.tools.BaseTool;

public class OpenAIBasedAgent extends BaseAgent {

    private String endpoint;
    private List<JsonNode> toolsSchemas;
    private HttpClient httpClient;
    private ObjectMapper objectMapper;

    public OpenAIBasedAgent(String model, String apiKey, List<BaseTool> tools, String systemPrompt) {
        super(model, apiKey, tools, systemPrompt);
        //TODO:
        // - Format `apiKey` as Bearer token and reassign `this.apiKey` (e.g. "Bearer " + apiKey)
        // - Set `endpoint` to `Constants.OPENAI_CHAT_COMPLETIONS_ENDPOINT`
        // - Initialize `httpClient` with `HttpClient.newHttpClient()` and `objectMapper` with `new ObjectMapper()`
        // - Build `toolsSchemas`: map each tool via `objectMapper.readTree(t.getOpenAiSchema())`
        // - Print `endpoint` and pretty-printed `toolsSchemas`
        this.apiKey = "Bearer " + apiKey;
        this.endpoint = Constants.OPENAI_CHAT_COMPLETIONS_ENDPOINT;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.toolsSchemas = tools.stream()
            .map(t -> {
                try {
                    return objectMapper.readTree(t.getOpenAiSchema());
                } catch (IOException e) {
                    throw new RuntimeException("Failed to read tool schema for tool: " + t.getName(), e);
                }
            }).toList();
        System.out.println("Endpoint: " + endpoint);
        System.out.println("Tools Schemas: " + toolsSchemas.stream()
            .map(JsonNode::toPrettyString)
            .collect(Collectors.joining("\n")));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Message getResponse(List<Message> messages, boolean printRequest) {
        //TODO:
        // - Build requestMessages: if systemPrompt set, prepend Message(Role.SYSTEM, systemPrompt).toMap()
        //   then map all messages via msg.toMap() — do NOT mutate the `messages` list itself
        // - Build requestData: model, requestMessages, toolsSchemas
        // - If `printRequest` — print endpoint and REQUEST payload (messages only)
        // - POST to endpoint with headers: Authorization (apiKey), Content-Type: application/json
        // - On HTTP 200: get choices[0], print RESPONSE
        //   - Extract content and tool_calls from choices[0]["message"]
        //   - Build ai_response: Message(ASSISTANT, content, null, null, toolCalls)
        //   - If finish_reason == "tool_calls": add ai_response to messages, call processToolCalls, recurse
        //   - Otherwise return ai_response; throw RuntimeException if choices is empty
        // - On non-200 — throw RuntimeException with status code and body
        try {
            List<Map<String, Object>> requestMessages = new ArrayList<>();
            if (systemPrompt != null && !systemPrompt.isBlank()) {
                requestMessages.add(new Message(Role.SYSTEM, systemPrompt).toMap());
            }
            requestMessages.addAll(messages.stream().map(Message::toMap).toList());

            Map<String, Object> requestData = new HashMap<>();
            requestData.put("model", model);
            requestData.put("messages", requestMessages);
            requestData.put("tools", toolsSchemas);

            if (printRequest) {
                System.out.println("Endpoint: " + endpoint);
                System.out.println("REQUEST payload: " + objectMapper.writeValueAsString(requestMessages));
            }

            String requestBody = objectMapper.writeValueAsString(requestData);
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Authorization", apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode jsonResponse = objectMapper.readTree(response.body());
                JsonNode choices = jsonResponse.path("choices");
                if (choices.isEmpty()) {
                    throw new RuntimeException("No choices returned in response");
                }
                JsonNode choice = choices.get(0);
                JsonNode messageNode = choice.path("message");
                String content = messageNode.path("content").asText("");
                List<Map<String, Object>> toolCalls = new ArrayList<>();
                if (messageNode.has("tool_calls")) {
                    toolCalls = objectMapper.convertValue(messageNode.path("tool_calls"), List.class);
                }
                String finishReason = choice.path("finish_reason").asText("");

                Message aiResponse = new Message(Role.ASSISTANT, content, null, null, toolCalls);

                System.out.println("RESPONSE: " + aiResponse);

                if ("tool_calls".equals(finishReason)) {
                    messages.add(aiResponse);
                    List<Message> toolMessages = processToolCalls(toolCalls);
                    messages.addAll(toolMessages);
                    return getResponse(messages, printRequest); // Recurse
                } else {
                    return aiResponse;
                }
            } else {
                throw new RuntimeException("Error: " + response.statusCode() + " " + response.body());
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Error during getResponse: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Message> processToolCalls(List<Map<String, Object>> toolCalls) throws IOException {
        //TODO:
        // - For each toolCall in toolCalls:
        //   - Extract toolCallId from toolCall.get("id")
        //   - Extract functionName from toolCall["function"]["name"]
        //   - Parse arguments from JSON string: objectMapper.readValue(toolCall["function"]["arguments"], Map.class)
        //   - Call `callTool(functionName, arguments)` and store the result
        //   - Add Message(Role.TOOL, result, toolCallId, functionName, null) to result list
        //   - Print function name and result
        // - Return the list of tool messages
        List<Message> toolMessages = new ArrayList<>();
        for (Map<String, Object> toolCall : toolCalls) {
            String toolCallId = (String) toolCall.get("id");
            Map<String, Object> function = (Map<String, Object>) toolCall.get("function");
            String functionName = (String) function.get("name");
            String argumentsJson = (String) function.get("arguments");
            Map<String, Object> arguments = objectMapper.readValue(argumentsJson, Map.class);
            String result = callTool(functionName, arguments);
            toolMessages.add(new Message(Role.TOOL, result, toolCallId, functionName, null));
            System.out.println("Tool call: " + functionName + ", Result: " +result);
        }
        return toolMessages;
    }
}
