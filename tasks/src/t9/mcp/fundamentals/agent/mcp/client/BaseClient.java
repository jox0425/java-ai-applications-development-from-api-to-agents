package t9.mcp.fundamentals.agent.mcp.client;

import commons.exceptions.TaskNotImplementedException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public abstract class BaseClient implements AutoCloseable {

    protected McpSyncClient mcpClient;
    protected final ObjectMapper objectMapper = new ObjectMapper();
    private ToolCallback[] toolCallbacks;

    public abstract void connect();

    /**
     * Must be called by subclasses in {@link #connect()} after {@code mcpClient.initialize()}.
     * Fetches available tools from the MCP server and caches them as Spring AI {@link ToolCallback}s.
     */
    protected void initToolCallbackProvider() {
        //TODO:
        // - create SyncMcpToolCallbackProvider.builder().addMcpClient(mcpClient).build()
        // - assign provider.getToolCallbacks() to this.toolCallbacks
        SyncMcpToolCallbackProvider provider = SyncMcpToolCallbackProvider.builder()
                .addMcpClient(mcpClient)
                .build();
        this.toolCallbacks = provider.getToolCallbacks();
    }

    public List<Map<String, Object>> getTools() {
        //TODO:
        // - iterate toolCallbacks; for each get ToolDefinition via cb.getToolDefinition()
        // - parse def.inputSchema() to Map via objectMapper.readValue()
        // - build Map with type="function" and nested "function" map (name, description, parameters)
        // - collect into result list and return
        List<Map<String, Object>> tools = new ArrayList<>();
        Arrays.stream(toolCallbacks).forEach(cb -> {
            try {
                var def = cb.getToolDefinition();
                Map<String, Object> inputSchema = objectMapper.readValue(def.inputSchema(), Map.class);
                Map<String, Object> toolMap = Map.of(
                        "type", "function",
                        "function", Map.of(
                                "name", def.name(),
                                "description", def.description(),
                                "parameters", inputSchema
                        )
                );
                tools.add(toolMap);
            } catch (Exception e) {
                throw new RuntimeException("Error processing tool callback: " + cb.getToolDefinition().name(), e);
            }
        });
        return tools;
    }

    public String callTool(String toolName, Map<String, Object> toolArgs) {
        //TODO:
        // - print tool invocation message with toolName and toolArgs
        // - find ToolCallback matching toolName in toolCallbacks; throw if not found
        // - call callback.call(objectMapper.writeValueAsString(toolArgs))
        // - print and return result; wrap exceptions in RuntimeException
        System.out.println("Invoking tool: " + toolName + " with args: " + toolArgs);
        ToolCallback callback = Arrays.stream(toolCallbacks)
                .filter(cb -> cb.getToolDefinition().name().equals(toolName))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Tool not found: " + toolName));
        try {
            String result = callback.call(objectMapper.writeValueAsString(toolArgs));
            // System.out.println("Tool result: " + result);
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Error calling tool: " + toolName, e);
        }
    }

    public List<McpSchema.Resource> getResources() {
        //TODO:
        // - call mcpClient.listResources().resources()
        // - catch exceptions: print warning message and return empty list
        try {
            return mcpClient.listResources().resources();
        } catch (Exception e) {
            System.err.println("Warning: Error fetching resources: " + e.getMessage());
            return List.of();
        }
    }

    public String getResource(String uri) {
        //TODO:
        // - call mcpClient.readResource(new McpSchema.ReadResourceRequest(uri))
        // - return "" if contents null or empty
        // - handle TextResourceContents and BlobResourceContents; return contents as string
        try {
            var response = mcpClient.readResource(new McpSchema.ReadResourceRequest(uri));
            if (response.contents() == null || response.contents().isEmpty()) {
                return "";
            }
            if (response.contents().getFirst() instanceof McpSchema.TextResourceContents textContents) {
                return textContents.text();
            } else if (response.contents().getFirst() instanceof McpSchema.BlobResourceContents blobContents) {
                return blobContents.blob();
            } else {
                throw new RuntimeException("Unsupported resource contents type: " + response.contents().getClass().getName());
            }
        } catch (Exception e) {
            System.err.println("Warning: Error fetching resource: " + e.getMessage());
            return "";
        }
    }

    public List<McpSchema.Prompt> getPrompts() {
        //TODO:
        // - call mcpClient.listPrompts().prompts()
        // - catch exceptions: print warning message and return empty list
        try {
            return mcpClient.listPrompts().prompts();
        } catch (Exception e) {
            System.err.println("Warning: Error fetching prompts: " + e.getMessage());
            return List.of();
        }
    }

    public String getPrompt(String name) {
        //TODO:
        // - call mcpClient.getPrompt(new McpSchema.GetPromptRequest(name, null))
        // - return "" if messages null or empty
        // - for each PromptMessage with TextContent, append tc.text() to StringBuilder
        // - return combined.toString().strip()
        try {
            var response = mcpClient.getPrompt(new McpSchema.GetPromptRequest(name, null));
            if (response.messages() == null || response.messages().isEmpty()) {
                return "";
            }
            StringBuilder combined = new StringBuilder();
            for (var message : response.messages()) {
                if (message.content() instanceof McpSchema.TextContent textContent) {
                    combined.append(textContent.text());
                }
            }
            return combined.toString().strip();
        } catch (Exception e) {
            System.err.println("Warning: Error fetching prompt: " + e.getMessage());
            return "";
        }
    }

    @Override
    public void close() {
        //TODO:
        // - if mcpClient != null, call mcpClient.closeGracefully()
        if (mcpClient != null) {
            mcpClient.closeGracefully();
        }
    }
}
