package t8.agent.task;

import commons.Constants;
import commons.model.Conversation;
import commons.model.Message;
import commons.model.Role;
import commons.user.service.UserServiceClient;
import t8.agent.task.agents.AnthropicBasedAgent;
import t8.agent.task.agents.BaseAgent;
import t8.agent.task.agents.OpenAIBasedAgent;
import t8.agent.task.tools.BaseTool;
import t8.agent.task.tools.WebSearchTool;
import t8.agent.task.tools.users.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class App {

    public static void main(String[] args) {
        //TODO:
        // - Create `UserServiceClient`
        // - Create list of all tools:
        //      - WebSearchTool (needs OPENAI_API_KEY),
        //      - GetUserByIdTool,
        //      - SearchUsersTool,
        //      - CreateUserTool,
        //      - UpdateUserTool,
        //      - DeleteUserTool
        // - Create OpenAIBasedAgent (or AnthropicBasedAgent) with all tools and Prompts.SYSTEM_PROMPT
        // - Create Conversation
        // - Print welcome message and run a Scanner loop until user types "exit":
        //   - Read user input, add Message(Role.USER, input) to conversation
        //   - Take a mutable copy of conversation messages; record its size before the call
        //   - Call agent.getResponse(messages, true) to get the AI reply
        //   - Sync any intermediate tool-call/tool-result messages back into conversation
        //   - Add the AI reply to conversation and print it
        UserServiceClient userServiceClient = new UserServiceClient();
        List<BaseTool> tools = new ArrayList<>();
        tools.add(new WebSearchTool(System.getenv(Constants.OPENAI_API_KEY)));
        tools.add(new GetUserByIdTool(userServiceClient));
        tools.add(new SearchUsersTool(userServiceClient));
        tools.add(new CreateUserTool(userServiceClient));
        tools.add(new UpdateUserTool(userServiceClient));
        tools.add(new DeleteUserTool(userServiceClient));
        var agent = new OpenAIBasedAgent(
                Constants.GPT_4_1_NANO,
                Constants.OPENAI_API_KEY,
                tools,
                Prompts.SYSTEM_PROMPT
        );

        System.out.println("Welcome to the AI Agent! Type your message below (type 'exit' to quit):");
        Scanner scanner = new Scanner(System.in);
        Conversation conversation = new Conversation();
        while (true) {
            System.out.print("You: ");
            String userInput = scanner.nextLine();
            if (userInput.equalsIgnoreCase("exit")) {
                break;
            }
            conversation.addMessage(new Message(Role.USER, userInput));
            List<Message> messagesCopy = new ArrayList<>(conversation.getMessages());
            int initialSize = messagesCopy.size();
            Message aiResponse = agent.getResponse(messagesCopy, true);
            // Sync any intermediate tool-call/tool-result messages back into conversation
            for (int i = initialSize; i < messagesCopy.size(); i++) {
                conversation.addMessage(messagesCopy.get(i));
            }
            conversation.addMessage(aiResponse);
            System.out.println("AI: " + aiResponse.content());
            }
    }
}
