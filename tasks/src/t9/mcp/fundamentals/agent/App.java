package t9.mcp.fundamentals.agent;

import commons.Constants;
import com.openai.models.chat.completions.ChatCompletionMessageParam;
import com.openai.models.chat.completions.ChatCompletionSystemMessageParam;
import com.openai.models.chat.completions.ChatCompletionUserMessageParam;
import commons.model.Message;
import commons.model.Role;
import io.modelcontextprotocol.spec.McpSchema;
import t9.mcp.fundamentals.agent.mcp.client.BaseClient;
import t9.mcp.fundamentals.agent.mcp.client.HttpClient;
import t9.mcp.fundamentals.agent.mcp.client.StdioClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class App {

    // Classpath of the compiled project for launching the STDIO server as a subprocess
    private static final String STDIO_SERVER_CLASS = "t9.mcp.fundamentals.mcp.server.StdioServerApp";

    public static void main(String[] args) throws Exception {
        // Switch active client by commenting/uncommenting:

        String javaClasspath = System.getProperty("java.class.path");

        // --- HTTP client (start HttpServer.java first): ---
        try (BaseClient mcpClient = new HttpClient("http://localhost:8005/mcp")) {
            runAgent(mcpClient);
        }

//        try (BaseClient mcpClient = new StdioClient(
//                null,
//                "java",
//                List.of("-cp", javaClasspath, STDIO_SERVER_CLASS),
//                null
//        )) {
//            runAgent(mcpClient);
//        }

        // --- Docker STDIO client: ---
//        try (BaseClient mcpClient = new StdioClient("mcp/duckduckgo:latest", null, null, null)) {
//            runAgent(mcpClient);
//        }
    }

    private static void runAgent(BaseClient mcpClient) {
        //TODO:
        // - call mcpClient.connect()
        // - list and print resources via mcpClient.getResources() (each r.uri() + r.description())
        // - list and print tools via mcpClient.getTools() (each function name + description)
        // - initialize messages list; add system message from Prompts.SYSTEM_PROMPT
        // - list prompts via mcpClient.getPrompts(); for each get content and add as user message
        //   with "## Prompt provided by MCP server:\n" + description + "\n" + content prefix
        // - create Agent with OPENAI_API_KEY, GPT model, tools, mcpClient
        // - run interactive chat loop: read input, add as user message, call agent.getCompletion(),
        //   add response; exit on "exit"
        mcpClient.connect();
        mcpClient.getResources().forEach(r -> System.out.println(r.uri() + ": " + r.description()));
        mcpClient.getTools().forEach(t -> {
            Map<String, Object> function = (Map<String, Object>) t.get("function");
            System.out.println(function.get("name") + ": " + function.get("description"));
        });
        var messages = new ArrayList<ChatCompletionMessageParam>();
        messages.add(ChatCompletionMessageParam.ofSystem(
            ChatCompletionSystemMessageParam.builder().content(Prompts.SYSTEM_PROMPT).build()
        ));
        List<McpSchema.Prompt> prompts = mcpClient.getPrompts();
        for (McpSchema.Prompt prompt : prompts) {
            System.out.println("  " + prompt.name() + ": " + prompt.description());
            String content = mcpClient.getPrompt(prompt.name());
            System.out.println(content);
            messages.add(ChatCompletionMessageParam.ofUser(
                ChatCompletionUserMessageParam.builder()
                    .content("## Prompt provided by MCP server:\n" + prompt.description() + "\n" + content)
                    .build()
            ));
        }
        String apiKey = Constants.OPENAI_API_KEY;
        Agent agent = new Agent(apiKey, Constants.GPT_5_4, mcpClient.getTools(), mcpClient);
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("\n> ");
            String userInput = scanner.nextLine().strip();

            if ("exit".equalsIgnoreCase(userInput)) {
                System.out.println("Exiting. Goodbye!");
                break;
            }

            messages.add(ChatCompletionMessageParam.ofUser(
                ChatCompletionUserMessageParam.builder().content(userInput).build()
            ));

            ChatCompletionMessageParam aiMessage = agent.getCompletion(messages);
            messages.add(aiMessage);
        }
    }
}
