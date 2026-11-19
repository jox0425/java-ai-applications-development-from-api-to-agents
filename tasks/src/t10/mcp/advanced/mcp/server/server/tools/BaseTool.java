package t10.mcp.advanced.mcp.server.server.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import commons.exceptions.TaskNotImplementedException;

import java.util.LinkedHashMap;
import java.util.Map;

public abstract class BaseTool {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public abstract String getName();

    public abstract String getDescription();

    public abstract String getInputSchema();

    public abstract String execute(Map<String, Object> arguments);

    public Map<String, Object> toMcpTool() {
        //TODO:
        // - create Map with tool metadata (name, description)
        // - parse and add inputSchema from getInputSchema() JSON string
        Map<String, Object> toolMap = new LinkedHashMap<>();
        toolMap.put("name", getName());
        toolMap.put("description", getDescription());
        try {
            toolMap.put("inputSchema", MAPPER.readValue(getInputSchema(), Map.class));
        } catch (Exception e) {
            throw new TaskNotImplementedException("Failed to parse inputSchema JSON for tool: " +getName());
        }
        return toolMap;
    }
}
