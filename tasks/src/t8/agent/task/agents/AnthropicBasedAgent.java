package t8.agent.task.agents;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import commons.Constants;
import commons.model.Message;
import commons.model.Role;
import t8.agent.task.tools.BaseTool;

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

public class AnthropicBasedAgent extends BaseAgent {

    private String endpoint;
    private List<JsonNode> toolsSchemas;
    private HttpClient httpClient;
    private ObjectMapper objectMapper;

    public AnthropicBasedAgent(String model, String apiKey, List<BaseTool> tools, String systemPrompt) {
        super(model, apiKey, tools, systemPrompt);
        //TODO:
        // - Set `endpoint` to `Constants.ANTHROPIC_ENDPOINT`
        // - Initialize `httpClient` with `HttpClient.newHttpClient()` and `objectMapper` with `new ObjectMapper()`
        // - Build `toolsSchemas`: map each tool via `objectMapper.readTree(t.getAnthropicSchema())`
        // - Print `endpoint` and pretty-printed `toolsSchemas`
        this.endpoint = Constants.ANTHROPIC_ENDPOINT;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.toolsSchemas = tools.stream()
            .map(t -> {
                try {
                    return objectMapper.readTree(t.getAnthropicSchema());
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
        // - Convert messages using `toAnthropicMessages(messages)`
        // - Build requestData: model, max_tokens=8096, anthropicMessages, toolsSchemas;
        //   add "system": systemPrompt if systemPrompt is set
        // - If `printRequest` — print endpoint and REQUEST payload (messages only)
        // - POST to endpoint with headers: x-api-key, anthropic-version: 2023-06-01, Content-Type: application/json
        // - On HTTP 200: parse content blocks and stop_reason; print RESPONSE
        //   - Extract text from first block where type == "text" (or null)
        //   - Extract tool_use blocks where type == "tool_use"
        //   - Build ai_response: Message(ASSISTANT, textContent, null, null, contentBlocks if tool_use else null)
        //   - If stop_reason == "tool_use": add ai_response to messages, call processToolCalls, recurse
        //   - Otherwise return ai_response
        // - On non-200 — throw RuntimeException with status code and body
        try {
            List<Map<String, Object>> anthropicMessages = toAnthropicMessages(messages);

            Map<String, Object> requestData = new HashMap<>();
            requestData.put("model", model);
            requestData.put("max_tokens", 8096);
            requestData.put("messages", anthropicMessages);
            requestData.put("tools", toolsSchemas);
            if (systemPrompt != null && !systemPrompt.isBlank()) {
                requestData.put("system", systemPrompt);
            }

            if (printRequest) {
                System.out.println("Endpoint: " + endpoint);
                System.out.println("REQUEST payload: " + objectMapper.writeValueAsString(anthropicMessages));
            }

            String requestBody = objectMapper.writeValueAsString(requestData);
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new RuntimeException("Error: " + response.statusCode() + " " + response.body());
            }

            JsonNode jsonResponse = objectMapper.readTree(response.body());
            JsonNode contentBlocks = jsonResponse.path("content");
            String textContent = null;
            List<Map<String, Object>> toolUseBlocks = new ArrayList<>();
            for (JsonNode block : contentBlocks) {
                String type = block.path("type").asText();
                if ("text".equals(type) && textContent == null) {
                    textContent = block.path("text").asText();
                } else if ("tool_use".equals(type)) {
                    toolUseBlocks.add(objectMapper.convertValue(block, Map.class));
                }
            }

            String stopReason = jsonResponse.path("stop_reason").asText();
            Message aiResponse = new Message(
                Role.ASSISTANT,
                textContent,
                null,
                null,
                toolUseBlocks.isEmpty() ? null : toolUseBlocks
            );
            System.out.println("RESPONSE: " + aiResponse);

            if ("tool_use".equals(stopReason)) {
                messages.add(aiResponse);
                messages.addAll(processToolCalls(toolUseBlocks));
                return getResponse(messages, printRequest);
            }
            return aiResponse;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Error during getResponse: " + e.getMessage(), e);
        }
    }

    private List<Map<String, Object>> toAnthropicMessages(List<Message> messages) {
        //TODO:
        // - Walk messages with index `i` using a while loop
        // - Role.TOOL: group consecutive TOOL messages into a single user message:
        //   {"role": "user", "content": [{"type": "tool_result", "tool_use_id": msg.toolCallId(), "content": msg.content()}, ...]}
        // - Role.ASSISTANT: use msg.toolCalls() as content if non-null, otherwise use msg.content()
        //   → {"role": "assistant", "content": <toolCalls or content>}
        // - Other roles: {"role": msg.role().getValue(), "content": msg.content()}
        // - Return the resulting list
        List<Map<String, Object>> anthropicMessages = new ArrayList<>();
        int i = 0;
        while (i < messages.size()) {
            Message message = messages.get(i);
            if (message.role() == Role.TOOL) {
                List<Map<String, Object>> toolResults = new ArrayList<>();
                while (i < messages.size() && messages.get(i).role() == Role.TOOL) {
                    Message toolMessage = messages.get(i++);
                    Map<String, Object> toolResult = new HashMap<>();
                    toolResult.put("type", "tool_result");
                    toolResult.put("tool_use_id", toolMessage.toolCallId());
                    toolResult.put("content", toolMessage.content());
                    toolResults.add(toolResult);
                }
                Map<String, Object> userMessage = new HashMap<>();
                userMessage.put("role", Role.USER.getValue());
                userMessage.put("content", toolResults);
                anthropicMessages.add(userMessage);
            } else {
                Map<String, Object> convertedMessage = new HashMap<>();
                convertedMessage.put("role", message.role().getValue());
                if (message.role() == Role.ASSISTANT && message.toolCalls() != null) {
                    convertedMessage.put("content", message.toolCalls());
                } else {
                    convertedMessage.put("content", message.content());
                }
                anthropicMessages.add(convertedMessage);
                i++;
            }
        }
        return anthropicMessages;
    }

    @SuppressWarnings("unchecked")
    private List<Message> processToolCalls(List<Map<String, Object>> toolUseBlocks) {
        //TODO:
        // - For each block in toolUseBlocks:
        //   - Extract toolUseId from block.get("id")
        //   - Extract functionName from block.get("name")
        //   - Get arguments (Map) from block.get("input") — already a Map, no JSON parsing needed
        //   - Call `callTool(functionName, arguments)` and store the result
        //   - Add Message(Role.TOOL, result, toolUseId, functionName, null) to result list
        //   - Print function name and result
        // - Return the list of tool messages
        List<Message> toolMessages = new ArrayList<>();
        for (Map<String, Object> toolUseBlock : toolUseBlocks) {
            String toolUseId = (String) toolUseBlock.get("id");
            String functionName = (String) toolUseBlock.get("name");
            Map<String, Object> arguments = (Map<String, Object>) toolUseBlock.get("input");
            String result = callTool(functionName, arguments);
            toolMessages.add(new Message(Role.TOOL, result, toolUseId, functionName, null));
            System.out.println("Tool call: " + functionName + ", Result: " + result);
        }
        return toolMessages;
    }
}
