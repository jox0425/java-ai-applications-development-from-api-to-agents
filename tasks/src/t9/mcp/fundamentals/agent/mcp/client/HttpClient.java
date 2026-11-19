package t9.mcp.fundamentals.agent.mcp.client;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;

public class HttpClient extends BaseClient {

    private final String mcpServerUrl;

    public HttpClient(String mcpServerUrl) {
        this.mcpServerUrl = mcpServerUrl;
    }

    @Override
    public void connect() {
        // https://java.sdk.modelcontextprotocol.io/latest/client/#streamable-http
        //TODO:
        // - split mcpServerUrl into baseUrl (before last '/') and endpoint (last path segment)
        // - build HttpClientStreamableHttpTransport with baseUrl, endpoint, JacksonMcpJsonMapper(objectMapper)
        // - create McpClient.sync(transport).build() and assign to mcpClient
        // - print connection start message, call mcpClient.initialize() then initToolCallbackProvider()
        // - print connected message
        int lastSlashIndex = mcpServerUrl.lastIndexOf('/');
        String baseUrl = mcpServerUrl.substring(0, lastSlashIndex);
        String endpoint = mcpServerUrl.substring(lastSlashIndex + 1);
        HttpClientStreamableHttpTransport transport = HttpClientStreamableHttpTransport.builder(baseUrl)
                .endpoint(endpoint)
                .jsonMapper(new JacksonMcpJsonMapper(objectMapper))
                .build();
        mcpClient = McpClient.sync(transport).build();
        System.out.println("Connecting to MCP server at " + mcpServerUrl + "...");
        mcpClient.initialize();
        initToolCallbackProvider();
        System.out.println("Connected to MCP server at " + mcpServerUrl);
    }
}
