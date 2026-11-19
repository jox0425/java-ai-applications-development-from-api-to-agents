package t10.mcp.advanced.mcp.server.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import t10.mcp.advanced.mcp.server.server.models.McpRequest;
import t10.mcp.advanced.mcp.server.server.models.McpResponse;

@RestController
public class Server {

    private static final String SESSION_ID_HEADER = "Mcp-Session-Id";

    private final UmsMcpServer mcpServer;
    private final ObjectMapper objectMapper;

    public Server(UmsMcpServer mcpServer) {
        this.mcpServer = mcpServer;
        this.objectMapper = new ObjectMapper();
    }

    @PostMapping(value = "/mcp", consumes = "application/json")
    public void handleMcp(
        @RequestBody McpRequest request,
        @RequestHeader(value = "Accept", required = false) String accept,
        @RequestHeader(value = SESSION_ID_HEADER, required = false) String sessionId,
        HttpServletResponse httpResponse
    ) throws IOException {
        // TODO:
        // - validate Accept header (must contain JSON and SSE)
        // - handle "initialize" method
        // - validate Mcp-Session-Id and retrieve the session
        // - handle "notifications/initialized" and check session readiness
        // - handle "tools/list" and "tools/call"
        // - write MCP responses to the SSE stream
        switch (request.getMethod() == null ? "" : request.getMethod()) {
            case "initialize":
                handleInitialize(request, accept, sessionId, httpResponse);
                break;
            case "notifications/initialized":
                handleInitialized(request, accept, sessionId, httpResponse);
                break;
            case "tools/list":
                handleToolsList(request, accept, sessionId, httpResponse);
                break;
            case "tools/call":
                handleToolsCall(request, accept, sessionId, httpResponse);
                break;
            default:
                handleUnknownMethod(request, accept, sessionId, httpResponse);
        }
    }

    private void handleInitialize(
        McpRequest request, String accept, String sessionId, HttpServletResponse response
    ) throws IOException {
        if (!validateAccept(request, accept, response)) {
            return;
        }

        var initResult = mcpServer.handleInitialize(request);
        writeSse(response, initResult.response(), initResult.sessionId());
    }

    private void handleInitialized(
        McpRequest request, String accept, String sessionId, HttpServletResponse response
    ) throws IOException {
        McpSession session = validateSession(request, accept, sessionId, response, false);
        if (session != null) {
            session.setReadyForOperation(true);
            response.setStatus(HttpServletResponse.SC_ACCEPTED);
        }
    }

    private void handleToolsList(
        McpRequest request, String accept, String sessionId, HttpServletResponse response
    ) throws IOException {
        if (validateSession(request, accept, sessionId, response, true) != null) {
            writeSse(response, mcpServer.handleToolsList(request), sessionId);
        }
    }

    private void handleToolsCall(
        McpRequest request, String accept, String sessionId, HttpServletResponse response
    ) throws IOException {
        if (validateSession(request, accept, sessionId, response, true) != null) {
            writeSse(response, mcpServer.handleToolsCall(request), sessionId);
        }
    }

    private void handleUnknownMethod(
        McpRequest request, String accept, String sessionId, HttpServletResponse response
    ) throws IOException {
        if (validateSession(request, accept, sessionId, response, true) != null) {
            writeError(response, HttpServletResponse.SC_NOT_FOUND,
                new McpResponse(request.getId(), Map.of(
                    "code", 404,
                    "message", "Unknown method: " + request.getMethod()
                )));
        }
    }

    private boolean validateAccept(
        McpRequest request, String accept, HttpServletResponse response
    ) throws IOException {
        if (isValidAcceptHeader(accept)) {
            return true;
        }

        writeError(response, HttpServletResponse.SC_BAD_REQUEST,
            new McpResponse(request.getId(), Map.of(
                "code", 400,
                "message", "Invalid Accept header. Must contain both application/json and text/event-stream."
            )));
        return false;
    }

    private McpSession validateSession(
        McpRequest request, String accept, String sessionId,
        HttpServletResponse response, boolean requireReady
    ) throws IOException {
        if (!validateAccept(request, accept, response)) {
            return null;
        }

        McpSession session = sessionId == null ? null : mcpServer.getSession(sessionId);
        if (session == null) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
                new McpResponse(request.getId(), Map.of(
                    "code", 401,
                    "message", "Invalid or missing Mcp-Session-Id header."
                )));
            return null;
        }

        if (requireReady && !session.isReadyForOperation()) {
            writeError(response, HttpServletResponse.SC_FORBIDDEN,
                new McpResponse(request.getId(), Map.of(
                    "code", 403,
                    "message", "Session is not ready for operation. Please send 'notifications/initialized' first."
                )));
            return null;
        }

        return session;
    }

    private void writeSse(HttpServletResponse httpResponse, McpResponse response, String sessionId)
        throws IOException {
        httpResponse.setContentType("text/event-stream");
        httpResponse.setHeader("Cache-Control", "no-cache");
        httpResponse.setHeader("Connection", "keep-alive");
        if (sessionId != null) {
            httpResponse.setHeader(SESSION_ID_HEADER, sessionId);
        }

        PrintWriter writer = httpResponse.getWriter();
        writer.write("data: " + objectMapper.writeValueAsString(response) + "\n\n");
        writer.write("data: [DONE]\n\n");
        writer.flush();
    }

    private void writeError(HttpServletResponse httpResponse, int status, McpResponse error)
        throws IOException {
        httpResponse.setStatus(status);
        httpResponse.setContentType("application/json");
        objectMapper.writeValue(httpResponse.getWriter(), error);
    }

    private boolean isValidAcceptHeader(String accept) {
        //TODO:
        // - check if accept header contains both application/json and text/event-stream
        return accept != null && accept.contains("application/json") && accept.contains("text/event-stream");
    }
}
