package t6.grounding.t2;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class InputVectorBasedGroundingApp {

    //TODO:
    // Define SYSTEM_PROMPT - instructs the LLM to act as a RAG-powered assistant:
    //   - The user message contains two sections: RAG CONTEXT and USER QUESTION
    //   - Answer ONLY based on the provided RAG CONTEXT and conversation history
    //   - If no relevant information exists in RAG CONTEXT, state that the question cannot be answered
    private static final String SYSTEM_PROMPT = """
        Act as a RAG-powered assistant. The user message contains two sections: RAG CONTEXT and USER QUESTION.
        Answer ONLY based on the provided RAG CONTEXT and conversation history. If no relevant information exists in RAG CONTEXT, state that the question cannot be answered.
            """;

    //TODO:
    // Define USER_PROMPT template with two placeholders:
    //   - {context} - the retrieved user data from vector store
    //   - {query}   - the user's question
    private static final String USER_PROMPT = """
        ## RAG CONTEXT:
        {context}
        ## USER QUESTION:
        {query}
            """;

    private final OpenAIClient openAiClient;
    private final SimpleVectorStore vectorStore;

    public InputVectorBasedGroundingApp(OpenAiEmbeddingModel embeddingModel) {
        this.openAiClient = OpenAIOkHttpClient.builder()
                .apiKey(Constants.OPENAI_API_KEY)
                .build();
        this.vectorStore = buildVectorStore(embeddingModel);
    }

    private SimpleVectorStore buildVectorStore(OpenAiEmbeddingModel embeddingModel) {
        //TODO:
        // - Print "🔎 Loading all users..."
        // - Fetch all users via new UserService().getAllUsers()
        // - Print "↗️ Formatting {count} user documents and creating embeddings..."
        // - Map users to Documents using Document.builder().id(String.valueOf(u.id())).text(u.toDocument()).build()
        // - Create SimpleVectorStore via SimpleVectorStore.builder(embeddingModel).build()
        // - Call addInParallel(store, documents, 50)
        // - Print "✅ Vectorstore is ready."
        // - Return store
        System.out.println("\uD83D\uDD0E Loading all users...");
        var allUsers = new UserService().getAllUsers();
        System.out.println("\u21D7\uFE0F Formatting " + allUsers.size() + " user documents and creating embeddings...");
        List<Document> documents = allUsers.stream()
                .map(u -> Document.builder()
                        .id(String.valueOf(u.id()))
                        .text(u.toDocument())
                        .build())
                .toList();
        var store = SimpleVectorStore.builder(embeddingModel).build();
        addInParallel(store, documents, 50);
        System.out.println("\u2705 Vectorstore is ready.");
        return store;
    }

    private static void addInParallel(SimpleVectorStore store, List<Document> documents, int batchSize) {
        //TODO:
        // - Split documents into batches of batchSize using subList (loop with i += batchSize)
        // - Create a list of CompletableFuture<Void> tasks via runAsync: each calls store.add(batch)
        // - Wait for all futures with CompletableFuture.allOf(...).join()
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < documents.size(); i += batchSize) {
            int end = Math.min(i + batchSize, documents.size());
            List<Document> batch = documents.subList(i, end);
            futures.add(CompletableFuture.runAsync(() -> store.add(batch)));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    private String retrieveContext(String query, int k, double minScore) {
        System.out.println("Retrieving context...");
        //TODO:
        // - Build SearchRequest with query, topK=k, similarityThreshold=minScore
        // - Call vectorStore.similaritySearch(request)
        // - For each result, print "Retrieved (Score: {score}): {text}" and collect text into contextParts
        // - Print separator of 100 "=" characters
        // - Return contextParts joined with "\n\n"
        var searchRequest = SearchRequest.builder()
                .query(query)
                .topK(k)
                .similarityThreshold(minScore)
                .build();
        var results = vectorStore.similaritySearch(searchRequest);
        List<String> contextParts = new ArrayList<>();
        for (var result : results) {
            String text = result.getText();
            double score = result.getScore();
            System.out.println("Retrieved (Score: " + score + "): " + text);
            contextParts.add(text);
            System.out.println("====================================================================================================");
        }
        return String.join("\n\n", contextParts);
    }

    private String augmentPrompt(String query, String context) {
        //TODO:
        // - Return USER_PROMPT with {context} and {query} replaced
        return USER_PROMPT.replace("{context}", context).replace("{query}", query);
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

        InputVectorBasedGroundingApp app = new InputVectorBasedGroundingApp(embeddingModel);

        System.out.println("Query samples:");
        System.out.println(" - I need user emails that filled with hiking and psychology");
        System.out.println(" - Who is John?");

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("\n> ");
            System.out.flush();
            if (!scanner.hasNextLine()) break;
            String query = scanner.nextLine().strip();
            if (query.isEmpty()) continue;
            if (query.equalsIgnoreCase("quit") || query.equalsIgnoreCase("exit")) break;

            //TODO:
            // - Call app.retrieveContext(query, 10, 0.1) and store in context
            // - Call app.augmentPrompt(query, context) and store in augmented
            // - Call app.generateAnswer(augmented) and print the answer
            var context = app.retrieveContext(query, 10, 0.1);
            var augmented = app.augmentPrompt(query, context);
            var answer = app.generateAnswer(augmented);
            System.out.println("Answer:\n" + answer);
        }
    }
}

// The problems with Vector based Grounding approach are:
//   - In current solution we fetched all users once, prepared Vector store (Embed takes money) but we didn't play
//     around the point that new users added and deleted every 5 minutes. (Actually, it can be fixed, we can create once
//     Vector store and with new request we will fetch all the users, compare new and deleted with version in Vector
//     store and delete the data about deleted users and add new users).
//   - Limit with top_k (we can set up to 100, but what if the real number of similarity search 100+?)
//   - With some requests works not so perfectly.
//   - Need to play with balance between top_k and score_threshold
// Benefits are:
//   - Similarity search by context
//   - Any input can be used for search
//   - Costs reduce
