package t6.grounding.t2;

import java.util.List;
import java.util.Scanner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import commons.Constants;
import t6.grounding.User;
import t6.grounding.UserService;

public class InputApiBasedGroundingApp {

    //TODO:
    // Define QUERY_ANALYSIS_PROMPT - instructs the LLM to act as a query analysis system:
    //   - Available search fields: name, surname, email
    //   - Analyze the user question and extract explicit search values
    //   - Map extracted values to the appropriate search fields
    //   - Only extract values that are clearly stated - do not infer or assume
    //   - Include examples: "Who is John?" → name: "John", "Find John Smith" → name: "John", surname: "Smith"
    private static final String QUERY_ANALYSIS_PROMPT = """
        Act as a query analysis system.
        You are given a user question and your task is to extract explicit search values for the following fields: name, surname, email.
        Only extract values that are clearly stated in the question. Do not infer or assume any values. If no explicit values are found, return an empty array.
          ## Examples:
            - "Who is John?" → name: "John"
            - "Find users with surname Smith" → surname: "Smith"
            - "Look for john@example.com" → email: "john@example.com"
            - "Find John Smith" → name: "John", surname: "Smith"
            - "I need user emails that filled with hiking" → No clear search parameters (return empty list)
        
            ## Response Format (always return valid JSON):
            {
              "search_request_parameters": [
                {"search_field": "name", "search_value": "John"},
                {"search_field": "surname", "search_value": "Smith"}
              ]
            }
            search_field must be one of: "name", "surname", "email"
        """;

    //TODO:
    // Define SYSTEM_PROMPT - instructs the LLM to act as a RAG-powered assistant:
    //   - The user message contains two sections: RAG CONTEXT and USER QUESTION
    //   - Answer ONLY based on the provided RAG CONTEXT and conversation history
    //   - If no relevant information exists in RAG CONTEXT, state that the question cannot be answered
    //   - Format user information clearly when presenting it
    private static final String SYSTEM_PROMPT = """
        Act as a RAG-powered assistant. The user message contains two sections: RAG CONTEXT and USER QUESTION.
        Answer ONLY based on the provided RAG CONTEXT and conversation history. If no relevant information exists in RAG CONTEXT, state that the question cannot be answered.
        Format user information clearly when presenting it.
        """;

    //TODO:
    // Define USER_PROMPT template with two placeholders:
    //   - {context} - the retrieved user data formatted as text
    //   - {query}   - the user's original question
    private static final String USER_PROMPT = """
        CONTEXT: {context}
        USER QUESTION: {query}
        """;

    private final OpenAIClient openAiClient;
    private final UserService userService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public InputApiBasedGroundingApp() {
        this.openAiClient = OpenAIOkHttpClient.builder()
            .apiKey(Constants.OPENAI_API_KEY)
            .build();
        this.userService = new UserService();
    }

    private List<User> retrieveContext(String userQuestion) {
        //TODO:
        // - Build ChatCompletionCreateParams with GPT_4_1_NANO, temperature=0.0, QUERY_ANALYSIS_PROMPT,
        //   userQuestion, and ResponseFormatJsonObject response format
        // - Call openAiClient.chat().completions().create(params), extract JSON string from first choice
        // - Parse JSON with objectMapper: read "search_request_parameters" array node
        // - If array is missing or empty: print "No specific search parameters found!" and return List.of()
        // - Iterate params: switch on "search_field" to assign name/surname/email variables
        // - Print search parameters and return userService.searchUsers(name, surname, email)
        // - Wrap checked exceptions in RuntimeException
        var params = ChatCompletionCreateParams.builder()
            .model(Constants.GPT_4_1_NANO)
            .temperature(0.0)
            .addSystemMessage(QUERY_ANALYSIS_PROMPT)
            .addUserMessage(userQuestion)
            .build();

        var completion = openAiClient.chat().completions().create(params);
        String responseJson = completion.choices().getFirst().message().content().orElse("{}");

        try {
            var root = objectMapper.readTree(responseJson);
            var searchParams = root.path("search_request_parameters");

            if (searchParams.isMissingNode() || !searchParams.isArray() || searchParams.isEmpty()) {
                System.out.println("No specific search parameters found!");
                return List.of();
            }

            String name = null, surname = null, email = null;
            for (var param : searchParams) {
                var field = param.path("search_field").asText();
                var value = param.path("search_value").asText();
                switch (field) {
                    case "name" -> name = value;
                    case "surname" -> surname = value;
                    case "email" -> email = value;
                }
            }

            System.out.printf("Searching with parameters: name=%s, surname=%s, email=%s%n", name, surname, email);
            return userService.searchUsers(name, surname, email);

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse search parameters: " + e.getMessage(), e);
        }
    }

    private String augmentPrompt(String userQuestion, List<User> context) {
        //TODO:
        // - Stream context, call User::toDocument on each, join with "\n" into contextStr
        // - Build augmented prompt by replacing {context} and {query} in USER_PROMPT
        // - Print augmented prompt
        // - Return augmented prompt
        var contextStr = context.stream()
            .map(User::toDocument)
            .reduce((a, b) -> a + "\n" + b)
            .orElse("");

        var augmentedPrompt = USER_PROMPT.replace("{context}", contextStr).replace("{query}", userQuestion);
        System.out.println("Augmented prompt:\n" + augmentedPrompt);
        return augmentedPrompt;
    }

    private String generateAnswer(String augmentedPrompt) {
        //TODO:
        // - Build ChatCompletionCreateParams with GPT_4O_MINI, temperature=0.0, SYSTEM_PROMPT and augmentedPrompt
        // - Call openAiClient.chat().completions().create(params)
        // - Return content from completion.choices().get(0).message().content() (default to "" if absent)
        var params = ChatCompletionCreateParams.builder()
            .model(Constants.GPT_4O_MINI)
            .temperature(0.0)
            .addSystemMessage(SYSTEM_PROMPT)
            .addUserMessage(augmentedPrompt)
            .build();
        var response = openAiClient.chat().completions().create(params);
        return response.choices().getFirst().message().content().orElse("");
    }

    public static void main(String[] args) {
        InputApiBasedGroundingApp app = new InputApiBasedGroundingApp();

        System.out.println("Query samples:");
        System.out.println(" - I need user emails that filled with hiking and psychology");
        System.out.println(" - Who is John?");
        System.out.println(" - Find users with surname Adams");
        System.out.println(" - Do we have smbd with name John that love painting?");

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("\n> ");
            System.out.flush();
            if (!scanner.hasNextLine()) {
                break;
            }
            String userQuestion = scanner.nextLine().strip();
            if (userQuestion.isEmpty()) {
                continue;
            }
            if (userQuestion.equalsIgnoreCase("quit") || userQuestion.equalsIgnoreCase("exit")) {
                break;
            }

            //TODO:
            // - Print "\n--- Retrieving context ---"
            // - Call app.retrieveContext(userQuestion) and store in context
            // - If context is not empty:
            //   - Print "\n--- Augmenting prompt ---"
            //   - Call app.augmentPrompt(userQuestion, context) and store in augmented
            //   - Print "\n--- Generating answer ---"
            //   - Call app.generateAnswer(augmented), print "\nAnswer: {answer}\n"
            // - Otherwise: print "\n--- No relevant information found ---"
            System.out.println("\n--- Retrieving context ---");
            var context = app.retrieveContext(userQuestion);
            if (!context.isEmpty()) {
                System.out.println("\n--- Augmenting prompt ---");
                var augmented = app.augmentPrompt(userQuestion, context);
                System.out.println("\n--- Generating answer ---");
                var answer = app.generateAnswer(augmented);
                System.out.println("\nAnswer: " + answer + "\n");
            } else {
                System.out.println("\n--- No relevant information found ---");
            }
        }
    }
}

// The problems with API based Grounding approach are:
//   - We need a Pre-Step to figure out what field should be used for search (Takes time)
//   - Values for search should be correct (✅ John -> ❌ Jonh)
//   - Is not so flexible
// Benefits are:
//   - We fetch actual data (new users added and deleted every 5 minutes)
//   - Costs reduce
