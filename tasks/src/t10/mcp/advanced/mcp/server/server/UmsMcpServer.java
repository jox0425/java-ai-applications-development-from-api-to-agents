package t10.mcp.advanced.mcp.server.server;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import commons.user.service.UserServiceClient;
import org.springframework.stereotype.Service;
import t10.mcp.advanced.mcp.server.server.models.McpRequest;
import t10.mcp.advanced.mcp.server.server.models.McpResponse;
import t10.mcp.advanced.mcp.server.server.tools.BaseTool;
import t10.mcp.advanced.mcp.server.server.tools.users.CreateUserTool;
import t10.mcp.advanced.mcp.server.server.tools.users.DeleteUserTool;
import t10.mcp.advanced.mcp.server.server.tools.users.GetUserByIdTool;
import t10.mcp.advanced.mcp.server.server.tools.users.SearchUsersTool;
import t10.mcp.advanced.mcp.server.server.tools.users.UpdateUserTool;

@Service
public class UmsMcpServer {

    private static final String PROTOCOL_VERSION = "2025-11-25";

    private final Map<String, McpSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, BaseTool> tools = new LinkedHashMap<>();

    public UmsMcpServer() {
        registerTools();
    }

    private void registerTools() {
        UserServiceClient client = new UserServiceClient();
        List.of(
            new GetUserByIdTool(client),
            new SearchUsersTool(client),
            new CreateUserTool(client),
            new UpdateUserTool(client),
            new DeleteUserTool(client)
        ).forEach(tool -> tools.put(tool.getName(), tool));
    }

    public McpSession getSession(String sessionId) {
        McpSession session = sessions.get(sessionId);
        if (session != null) {
            session.updateActivity();
        }
        return session;
    }

    public record InitResult(McpResponse response, String sessionId) {}

    public InitResult handleInitialize(McpRequest request) {
        //TODO:
        // - generate unique session ID and capabilities (tools, resources, prompts)
        // - build and return InitResult with protocolVersion, capabilities and serverInfo
        String sessionId = UUID.randomUUID().toString();
        McpResponse response = new McpResponse(request.getId(), Map.of(
            "protocolVersion", PROTOCOL_VERSION,
            "capabilities", Map.of(
                "tools", tools.keySet(),
                "resources", List.of(),
                "prompts", List.of()
            ),
            "serverInfo", Map.of(
                "name", "UMS MCP Server",
                "version", "1.0.0"
            )
        ));
        sessions.put(sessionId, new McpSession(sessionId));
        return new InitResult(response, sessionId);
    }

    public McpResponse handleToolsList(McpRequest request) {
        //TODO:
        // - collect all registered tools and convert them to MCP tool format using BaseTool.toMcpTool()
        // - return McpResponse with the list of tools
        List<Map<String, Object>> toolList = tools.values().stream()
            .map(BaseTool::toMcpTool)
            .toList();
        return new McpResponse(request.getId(), Map.of("tools", toolList));
    }

    public McpResponse handleToolsCall(McpRequest request) {
        try {
            Map<String, Object> params = request.getParams();
            if (params == null) {
                throw new IllegalArgumentException("Missing request parameters");
            }

            Object nameValue = params.get("name");
            if (!(nameValue instanceof String toolName) || toolName.isBlank()) {
                throw new IllegalArgumentException("Missing or invalid tool name");
            }

            BaseTool tool = tools.get(toolName);
            if (tool == null) {
                throw new IllegalArgumentException("Tool not found: " + toolName);
            }

            Map<String, Object> toolArgs = new LinkedHashMap<>();
            Object argsValue = params.get("arguments");
            if (argsValue != null) {
                if (!(argsValue instanceof Map<?, ?> args)) {
                    throw new IllegalArgumentException("Tool arguments must be an object");
                }
                for (Map.Entry<?, ?> entry : args.entrySet()) {
                    if (!(entry.getKey() instanceof String key)) {
                        throw new IllegalArgumentException("Tool argument names must be strings");
                    }
                    toolArgs.put(key, entry.getValue());
                }
            }

            String resultText = tool.execute(toolArgs);
            return new McpResponse(request.getId(), Map.of(
                "content", List.of(Map.of(
                    "type", "text",
                    "text", String.valueOf(resultText)
                ))
            ));
        } catch (RuntimeException e) {
            String message = e.getMessage();
            return new McpResponse(request.getId(), Map.of(
                "content", List.of(Map.of(
                    "type", "text",
                    "text", message == null ? "Tool call failed" : message
                )),
                "isError", true
            ));
        }
    }
}
