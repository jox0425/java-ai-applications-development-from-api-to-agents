package t6.grounding.t3;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ResponseFormatJsonObject;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import commons.Constants;
import commons.exceptions.TaskNotImplementedException;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import t6.grounding.User;
import t6.grounding.UserService;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * HOBBIES SEARCHER:
 * Searches users by hobbies and provides their full info:
 * Input: I need to gather people that love to go to mountains
 * Output:
 *    rock climbing: [{full user info},...],
 *    hiking: [{full user info},...],
 *    camping: [{full user info},...]
 */
public class InOutGroundingApp {

    //TODO:
    // Define SYSTEM_PROMPT - instructs the LLM to group users by hobby (Named Entity Extraction):
    //   - Context will contain user IDs and about_me sections from the vector store
    //   - Group users by hobby: each matching user ID should appear under its relevant hobby
    //   - Return only valid JSON matching this format:
    //     {"grouping_results": [{"hobby": "hiking", "user_ids": [1, 2, 3]}, {"hobby": "camping", "user_ids": [4, 5]}]}
    private static final String SYSTEM_PROMPT = """
            Group the following user IDs by hobby based on their about_me sections. Each user ID should appear under its relevant hobby. Return only valid JSON in the following format:
            {
                "grouping_results": [
                    {"hobby": "hiking", "user_ids": [1, 2, 3]},
                    {"hobby": "camping", "user_ids": [4, 5]}
                ]
            }
            """;

    //TODO:
    // Define USER_PROMPT template with two placeholders:
    //   - {context} - the retrieved user hobby data from vector store (id + about_me only)
    //   - {query}   - the user's search question
    private static final String USER_PROMPT = """
            You are given the following context containing user IDs and their about_me sections:
            {context}
            Based on this context, answer the following query by grouping user IDs by hobby:
            {query}
            """;

    private final OpenAIClient openAiClient;
    private final UserService userService;
    private final SimpleVectorStore vectorStore;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Set<String> knownUserIds = new HashSet<>();

    public InOutGroundingApp(OpenAiEmbeddingModel embeddingModel) {
        this.openAiClient = OpenAIOkHttpClient.builder()
                .apiKey(Constants.OPENAI_API_KEY)
                .build();
        this.userService = new UserService();
        this.vectorStore = SimpleVectorStore.builder(embeddingModel).build();
        initializeVectorStore();
    }

    private void initializeVectorStore() {
        //TODO:
        // - Print "🔍 Loading all users for initial vectorstore..."
        // - Fetch all users via userService.getAllUsers()
        // - Map users to Documents using Document.builder().id(String.valueOf(u.id())).text(u.toHobbyDocument()).build()
        //   Note: use toHobbyDocument() (embeds only id + about_me) to reduce embedding context
        // - Call addInParallel(vectorStore, documents, 50)
        // - Add all user IDs to knownUserIds set
        // - Print "Setup FINISHED"
        System.out.println("🔍 Loading all users for initial vectorstore...");
        var allUsers = userService.getAllUsers();
        var documents = allUsers.stream()
                .map(u -> Document.builder()
                        .id(String.valueOf(u.id()))
                        .text(u.toHobbyDocument())
                        .build())
                .toList();
        addInParallel(vectorStore, documents, 50);
        knownUserIds.addAll(allUsers.stream().map(u -> String.valueOf(u.id())).toList());
        System.out.println("Setup FINISHED");
    }

    private void updateVectorStore() {
        //TODO: (adaptive sync — called before every retrieval)
        // - Fetch current users via userService.getAllUsers(), build a Map<String, User> keyed by id string
        // - Compute newIds: currentIds minus knownUserIds
        // - Compute deletedIds: knownUserIds minus currentIds
        // - If deletedIds not empty: call vectorStore.delete(new ArrayList<>(deletedIds)),
        //   remove from knownUserIds, and print how many were deleted
        // - If newIds not empty: map new IDs to Documents using toHobbyDocument(),
        //   call addInParallel(vectorStore, newDocuments, 50),
        //   add to knownUserIds, and print how many were added
        var currentUsers = userService.getAllUsers();
        var currentUserMap = currentUsers.stream()
                .collect(Collectors.toMap(u -> String.valueOf(u.id()), u -> u));
        var currentIds = currentUserMap.keySet();
        var newIds = new HashSet<>(currentIds);
        newIds.removeAll(knownUserIds);
        var deletedIds = new HashSet<>(knownUserIds);
        deletedIds.removeAll(currentIds);
        if (!deletedIds.isEmpty()) {
            vectorStore.delete(new ArrayList<>(deletedIds));
            knownUserIds.removeAll(deletedIds);
            System.out.println("Deleted " + deletedIds.size() + " users from vector store.");
        }
        if (!newIds.isEmpty()) {
            var newDocuments = newIds.stream()
                    .map(id -> currentUserMap.get(id))
                    .filter(Objects::nonNull)
                    .map(u -> Document.builder()
                            .id(String.valueOf(u.id()))
                            .text(u.toHobbyDocument())
                            .build())
                    .toList();
            addInParallel(vectorStore, newDocuments, 50);
            knownUserIds.addAll(newIds);
            System.out.println("Added " + newIds.size() + " new users to vector store.");
        }
    }

    private String retrieveContext(String query, int k, double minScore) {
        //TODO:
        // - Call updateVectorStore() to sync additions/deletions before each retrieval
        // - Print "Retrieving context..."
        // - Build SearchRequest with query, topK=k, similarityThreshold=minScore
        // - Call vectorStore.similaritySearch(request)
        // - For each result, print "Retrieved (Score: {score}): {text}" and collect text into contextParts
        // - Print separator of 100 "=" characters
        // - Return contextParts joined with "\n\n"
        updateVectorStore();
        System.out.println("Retrieving context...");
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(k)
                .similarityThreshold(minScore)
                .build();
        var results = vectorStore.similaritySearch(request);
        var contextParts = new ArrayList<String>();
        for (var result : results) {
            System.out.printf("Retrieved (Score: %.4f): %s%n", result.getScore(), result.getText());
            contextParts.add(result.getText());
            System.out.println("====================================================================================================");
        }
        return String.join("\n\n", contextParts);
    }

    private String augmentPrompt(String query, String context) {
        //TODO:
        // - Return USER_PROMPT with {context} and {query} replaced
        return USER_PROMPT.replace("{context}", context).replace("{query}", query);
    }

    private List<GroupingResult> generateGroupingResults(String augmentedPrompt) {
        //TODO:
        // - Build ChatCompletionCreateParams with GPT_4_1_NANO, temperature=0.0, SYSTEM_PROMPT,
        //   augmentedPrompt, and ResponseFormatJsonObject response format
        // - Call openAiClient.chat().completions().create(params), extract JSON string from first choice
        // - Parse JSON with objectMapper: read "grouping_results" array node
        // - If array is missing or not an array: return List.of()
        // - For each grouping node: extract "hobby" string and "user_ids" array (each element as int)
        // - Collect into List<GroupingResult> and return
        // - Wrap checked exceptions in RuntimeException
        try {
            ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                    .model("gpt-4.1-nano")
                    .temperature(0.0)
                    .addSystemMessage(SYSTEM_PROMPT)
                    .addUserMessage(augmentedPrompt)
                    .responseFormat(ResponseFormatJsonObject.builder().build())
                    .build();
            var response = openAiClient.chat().completions().create(params);
            String jsonString = response.choices().getFirst().message().content().get();
            JsonNode rootNode = objectMapper.readTree(jsonString);
            JsonNode groupingResultsNode = rootNode.get("grouping_results");
            if (groupingResultsNode == null || !groupingResultsNode.isArray()) {
                return List.of();
            }
            List<GroupingResult> groupingResults = new ArrayList<>();
            for (JsonNode groupingNode : groupingResultsNode) {
                String hobby = groupingNode.get("hobby").asText();
                List<Integer> userIds = new ArrayList<>();
                for (JsonNode idNode : groupingNode.get("user_ids")) {
                    userIds.add(idNode.asInt());
                }
                groupingResults.add(new GroupingResult(hobby, userIds));
            }
            return groupingResults;
        } catch (Exception e) {
            throw new RuntimeException("Error generating grouping results", e);
        }
    }

    private void groundResponse(List<GroupingResult> groupingResults) {
        //TODO: (output grounding — verify IDs exist and fetch full user data)
        // - For each GroupingResult: print "Hobby: {hobby}"
        // - Stream result.userIds(), map each id to userService.getUser(id) (returns Optional<User>)
        // - Flatten with Optional::stream to skip absent users (validates IDs against live data)
        // - Print the found users and a "----------" separator
        for (GroupingResult result : groupingResults) {
            System.out.println("Hobby: " + result.hobby());
            List<User> foundUsers = result.userIds().stream()
                    .map(userService::getUser)
                    .flatMap(Optional::stream)
                    .toList();
            for (User user : foundUsers) {
                System.out.println(user);
            }
            System.out.println("----------");
        }
    }

    private static void addInParallel(SimpleVectorStore store, List<Document> documents, int batchSize) {
        List<List<Document>> batches = new ArrayList<>();
        for (int i = 0; i < documents.size(); i += batchSize) {
            batches.add(documents.subList(i, Math.min(i + batchSize, documents.size())));
        }
        List<CompletableFuture<Void>> futures = batches.stream()
                .map(batch -> CompletableFuture.runAsync(() -> store.add(batch)))
                .toList();
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    private record GroupingResult(String hobby, List<Integer> userIds) {}

    public static void main(String[] args) {
        OpenAiEmbeddingModel embeddingModel = new OpenAiEmbeddingModel(
                OpenAiApi.builder()
                        .apiKey(Constants.OPENAI_API_KEY)
                        .build(),
                MetadataMode.EMBED,
                OpenAiEmbeddingOptions.builder()
                        .model("text-embedding-3-small")
                        .dimensions(384)
                        .build()
        );

        InOutGroundingApp app = new InOutGroundingApp(embeddingModel);

        System.out.println("Query samples:");
        System.out.println(" - I need people who love to go to mountains");
        System.out.println(" - Find people who love to watch stars and night sky");
        System.out.println(" - I need people to go to fishing together");

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("\n> ");
            System.out.flush();
            if (!scanner.hasNextLine()) break;
            String query = scanner.nextLine().strip();
            if (query.isEmpty()) continue;
            if (query.equalsIgnoreCase("quit") || query.equalsIgnoreCase("exit")) break;

            //TODO:
            // - Call app.retrieveContext(query, 100, 0.2) for input grounding (semantic search)
            // - Call app.augmentPrompt(query, context)
            // - Call app.generateGroupingResults(augmented) to get hobby → user IDs groupings from LLM
            // - Call app.groundResponse(groupingResults) for output grounding (fetches live user data)
            String context = app.retrieveContext(query, 100, 0.2);
            String augmentedPrompt = app.augmentPrompt(query, context);
            List<GroupingResult> groupingResults = app.generateGroupingResults(augmentedPrompt);
            app.groundResponse(groupingResults);
        }
    }
}
