package t8.agent.task.tools;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import commons.exceptions.TaskNotImplementedException;

import java.io.IOException;
import java.util.Map;

public abstract class BaseTool {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public abstract String execute(Map<String, Object> arguments);

    public abstract String getName();

    public abstract String getDescription();

    public abstract String getInputSchema();

    public String getOpenAiSchema() throws JsonProcessingException {
        //TODO:
        // https://developers.openai.com/api/docs/guides/function-calling#defining-functions
        // - Build a "function" ObjectNode with getName(), getDescription(), and getInputSchema() as "parameters"
        // - Wrap it in a root ObjectNode with "type": "function" and "function": <function node>
        // - Return JSON string via MAPPER.writeValueAsString()
        var rootNode = MAPPER.createObjectNode();
        var functionNode = MAPPER.createObjectNode();
        functionNode.put("name", getName());
        functionNode.put("description", getDescription());
        functionNode.set("parameters", MAPPER.readTree(getInputSchema()));
        rootNode.put("type", "function");
        rootNode.set("function", functionNode);
        return MAPPER.writeValueAsString(rootNode);
    }

    public String getAnthropicSchema() {
        //TODO:
        // https://platform.claude.com/docs/en/api/messages/create#create.tools
        // - Build ObjectNode with getName() as "name", getDescription() as "description",
        //   and getInputSchema() parsed as "input_schema"
        // - Return JSON string via MAPPER.writeValueAsString()
        var rootNode = MAPPER.createObjectNode();
        rootNode.put("name", getName());
        rootNode.put("description", getDescription());
        try {
            rootNode.set("input_schema", MAPPER.readTree(getInputSchema()));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse input schema", e);
        }
        return rootNode.toString();
    }
}
