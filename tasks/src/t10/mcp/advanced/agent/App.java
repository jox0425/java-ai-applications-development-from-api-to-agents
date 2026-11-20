package t10.mcp.advanced.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.models.chat.completions.ChatCompletionMessageParam;
import com.openai.models.chat.completions.ChatCompletionUserMessageParam;
import commons.Constants;
import t10.mcp.advanced.agent.clients.McpClient;

public class App {

    public static void main(String[] args) throws Exception {
        //TODO:
        // - initialize McpClient (or CustomMcpClient) with server URL
        // - fetch available tools from MCP client
        // - print tools details using objectMapper
        // - initialize Agent with API key, model, tools, and mcpClient
        // - implement chat loop: read user input, add to messages, get and print agent completion
        McpClient mcpClient = McpClient.create("http://localhost:8006/mcp");
        List<Map<String, Object>> tools = mcpClient.getTools();
        ObjectMapper objectMapper = new ObjectMapper();
        System.out.println("Available tools:");
        for (Map<String, Object> tool : tools) {
            String toolJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(tool);
            System.out.println(toolJson);
        }
        var agent = new Agent(Constants.OPENAI_API_KEY, Constants.GPT_5_4, tools, mcpClient);
        List<ChatCompletionMessageParam> messages = new ArrayList<>();
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("User: ");
            String userInput = scanner.nextLine();
            if (userInput.equalsIgnoreCase("exit")) {
                break;
            }
            messages.add(ChatCompletionMessageParam.ofUser(
                ChatCompletionUserMessageParam.builder().content(userInput).build()
            ));
            var agentResponse = agent.getCompletion(messages);
            System.out.println("Agent: " + agentResponse.toString());
            messages.add(agentResponse);
        }
        System.out.print("Bye!");
    }
}

// Check if Arkadiy Dobkin present as a user, if not then search info about him in the web and add him
