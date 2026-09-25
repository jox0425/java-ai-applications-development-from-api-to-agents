package t6.grounding.t1;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.completions.CompletionUsage;
import commons.Constants;
import t6.grounding.User;
import t6.grounding.UserService;

public class NoGroundingApp {

    //TODO:
    //  Define BATCH_SYSTEM_PROMPT - instructs the LLM to act as a user search assistant:
    //    - Analyze the search criteria from the user question
    //    - Examine each user in the provided list and determine if they match
    //    - Return full details of matching users in their original format
    //    - Return exactly "NO_MATCHES_FOUND" if no users match
    private static final String BATCH_SYSTEM_PROMPT = """
        You are a user search assistant. Your task is the following:
            - Analyze the search criteria from the user question
            - Examine each user in the provided list and determine if they match
            - Return full details of matching users in their original format
            - Return exactly "NO_MATCHES_FOUND" if no users match
        """;

    //TODO:
    // Define FINAL_SYSTEM_PROMPT - instructs the LLM to compile final search results:
    //   - Review all batch search results
    //   - Combine and deduplicate matching users found across batches
    //   - Present results in a clear, organized manner
    private static final String FINAL_SYSTEM_PROMPT = """
        Your task is to complete the final search results in the following way:
           - Review all batch search results
           - Combine and deduplicate matching users found across batches
           - Present results in a clear, organized manner
        """;

    //TODO:
    // Define USER_PROMPT template with two placeholders:
    //   - {context} - the formatted user data
    //   - {query}   - the user's search question
    private static final String USER_PROMPT = """
        ## USER DATA:
        {context}
        
        ## SEARCH QUERY:\s
        {query}""";

    private final OpenAIClient openAiClient;
    private final UserService userService;
    private final AtomicInteger totalTokens = new AtomicInteger(0);
    private final List<Integer> batchTokens = new CopyOnWriteArrayList<>();

    public NoGroundingApp() {
        this.openAiClient = OpenAIOkHttpClient.builder()
            .apiKey(Constants.OPENAI_API_KEY)
            .build();
        this.userService = new UserService();
    }

    private String generateResponse(String systemPrompt, String userMessage) {
        System.out.println("Processing...");
        //TODO:
        // - Build ChatCompletionCreateParams with GPT_4_1_NANO, temperature=0.0, systemPrompt and userMessage
        // - Call openAiClient.chat().completions().create(params)
        // - Extract total tokens from completion.usage() (default to 0 if absent), add to totalTokens and batchTokens
        // - Extract content string from completion.choices().get(0).message().content()
        // - Print response content and token count
        // - Return content string
        var params = new ChatCompletionCreateParams.Builder()
            .model(Constants.GPT_4_1_NANO)
            .temperature(0.0)
            .addSystemMessage(systemPrompt)
            .addUserMessage(userMessage)
            .build();
        var completion = openAiClient.chat().completions().create(params);

        completion.usage()
            .map(CompletionUsage::totalTokens)
            .map(Long::intValue)
            .ifPresent(total -> {
                totalTokens.addAndGet(total);
                batchTokens.add(total);
            });

        var message = completion.choices().getFirst().message().content();
        message.ifPresent(System.out::println);
        System.out.println("Total tokens: " + totalTokens.get());
        return message.orElse(null);
    }

    public void run(String userQuestion) {
        //TODO:
        // - Print "\n--- Searching user database ---"
        // - Fetch all users via userService.getAllUsers()
        // - Split users into batches of 100 using subList (loop with i += 100)
        // - Build a list of CompletableFuture<String> tasks via supplyAsync: each calls generateResponse
        //   with BATCH_SYSTEM_PROMPT and USER_PROMPT formatted using User::toDocument joined by "\n"
        //   (replace {context} with retrieved context and {query} with userQuestion)
        // - Wait for all futures with CompletableFuture.allOf(...).join(), then collect results
        // - Print "\n--- Compiling results ---"
        // - Filter batch results to keep only those where strip() is not "NO_MATCHES_FOUND"
        // - Print "\n=== SEARCH RESULTS ==="
        // - If relevant results exist: join with "\n\n" and call generateResponse with FINAL_SYSTEM_PROMPT
        // - Otherwise: print "No users found" message and suggest refinement
        // - Print "\n=== Performance ===" with total API calls (batchTokens.size()) and totalTokens
        System.out.println("\n--- Searching user database ---");
        var users = userService.getAllUsers();

        List<List<User>> batches = new ArrayList<>();
        for (int i = 0; i < users.size(); i += 100) {
            batches.add(users.subList(i, Math.min(i + 100, users.size())));
        }

        List<CompletableFuture<String>> futures = batches.stream()
            .map(batch -> CompletableFuture.supplyAsync(() -> generateResponse(
                BATCH_SYSTEM_PROMPT,
                USER_PROMPT
                    .replace("{context}", batch.stream()
                        .map(User::toDocument)
                        .collect(Collectors.joining("\n")))
                    .replace("{query}", userQuestion)
            )))
            .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        List<String> batchResults = futures.stream()
            .map(CompletableFuture::join)
            .toList();

        System.out.println("\n--- Compiling results ---");

        List<String> relevantResults = batchResults.stream()
            .filter(r -> !r.strip().equals("NO_MATCHES_FOUND"))
            .toList();

        System.out.println("\n=== SEARCH RESULTS ===");
        if (!relevantResults.isEmpty()) {
            String combined = String.join("\n\n", relevantResults);
            generateResponse(
                FINAL_SYSTEM_PROMPT,
                "SEARCH RESULTS:\n" + combined + "\n\nORIGINAL QUERY: " + userQuestion
            );
        } else {
            System.out.println("No users found matching '" + userQuestion + "'");
            System.out.println("\nTry refining your search or using different keywords.");
        }

        System.out.println("\n=== Performance ===");
        System.out.println("Total API calls: " + batchTokens.size());
        System.out.println("Total tokens: " + totalTokens.get());
    }

    public static void main(String[] args) {
        NoGroundingApp app = new NoGroundingApp();

        System.out.println("Query samples:");
        System.out.println(" - Do we have someone with name John that loves traveling?");

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("\n> ");
            System.out.flush();
            if (!scanner.hasNextLine()) {
                break;
            }
            String query = scanner.nextLine().strip();
            if (query.isEmpty()) {
                continue;
            }
            app.run(query);
        }
    }
}

// The problems with No Grounding approach are:
//   - If we load whole users as context in one request to LLM we will hit context window
//   - Huge token usage == Higher price per request
//   - Added + one chain in flow where original user data can be changed by LLM (before final generation)
// User Question -> Get all users -> ‼️parallel search of possible candidates‼️ -> probably changed original context -> final generation
