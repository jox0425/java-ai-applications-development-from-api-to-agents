package t4.rag.basics.spring;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Scanner;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import commons.Constants;
import commons.exceptions.TaskNotImplementedException;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.core.io.FileSystemResource;

public class SpringRagApp {

    private static final String SYSTEM_PROMPT = """
        You are a RAG-powered assistant that assists users with their questions about microwave usage.
        
        ## Structure of User message:
        `RAG CONTEXT` - Retrieved documents relevant to the query.
        `USER QUESTION` - The user's actual question.
        
        ## Instructions:
        - Use information from `RAG CONTEXT` as context when answering the `USER QUESTION`.
        - Cite specific sources when using information from the context.
        - Answer ONLY based on conversation history and RAG context.
        - If no relevant information exists in `RAG CONTEXT` or conversation history, state that you cannot answer the question.
        """;

    private static final String USER_PROMPT_TEMPLATE =
        "##RAG CONTEXT:\n{context}\n\n\n##USER QUESTION: \n{query}";

    private static final String MANUAL_PATH = "tasks/src/t4/rag/basics/microwave_manual.txt";
    private static final Path INDEX_PATH = Paths.get("tasks/src/t4/rag/basics/spring/microwave_index.json");

    private final OpenAiEmbeddingModel embeddingModel;
    private final OpenAIClient openAiClient;
    private final SimpleVectorStore vectorStore;

    private SpringRagApp(OpenAiEmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
        this.openAiClient = OpenAIOkHttpClient.builder()
            .apiKey(Constants.OPENAI_API_KEY)
            .build();
        this.vectorStore = setupVectorStore();
    }

    private SimpleVectorStore setupVectorStore() {
        //TODO:
        // - Print a startup message
        // - Build a SimpleVectorStore using SimpleVectorStore.builder(embeddingModel).build() https://docs.spring.io/spring-ai/docs/current/api/org/springframework/ai/vectorstore/SimpleVectorStore.html
        // - Check if INDEX_PATH already exists on disk
        // - If yes: load the index using store.load(INDEX_PATH.toFile()) and print a confirmation message
        // - If no: call populateStore(store)
        // - Return the store
        System.out.println("Setting up vector store");
        var simpleVectorStore = SimpleVectorStore.builder(embeddingModel).build();

        if (Files.exists(INDEX_PATH)) {
            simpleVectorStore.load(INDEX_PATH.toFile());
            System.out.print("Vector store index loaded.");
        } else {
            populateStore(simpleVectorStore);
        }
        return simpleVectorStore;
    }

    private void populateStore(SimpleVectorStore store) {
        //TODO:
        // - Print a loading message
        // - Load documents using TextReader(new FileSystemResource(MANUAL_PATH)).get() https://docs.spring.io/spring-ai/reference/api/etl-pipeline.html#_text
        // - Print a splitting message
        // - Build a TokenTextSplitter (chunkSize=75, minChunkSizeChars=50, minChunkLengthToEmbed=5, maxNumChunks=10000, keepSeparator=true)
        //   and split the documents https://docs.spring.io/spring-ai/reference/api/etl-pipeline.html#_using_the_builder_pattern
        // - Print the number of chunks created
        // - Print an embedding/indexing message
        // - Add chunks to the store using store.add(chunks)
        // - Save the index using store.save(INDEX_PATH.toFile())
        // - Print saved and success messages
        System.out.println("Populating vector store...");
        var docs = new TextReader(new FileSystemResource(MANUAL_PATH)).get();
        System.out.println("Splitting documents...");
        var splitter = TokenTextSplitter.builder()
            .withChunkSize(75)
            .withMinChunkSizeChars(50)
            .withMinChunkLengthToEmbed(5)
            .withMaxNumChunks(10000)
            .withKeepSeparator(true)
            .build();

        var chunks = splitter.split(docs);

        System.out.printf("Number of chunks created:  %d%n", chunks.size());

        System.out.println("Embedding/indexing...");
        store.add(chunks);
        store.save(INDEX_PATH.toFile());
        System.out.println("Embedding/indexing done.");
    }

    private String retrieveContext(String query, int k, double minScore) {
        //TODO:
        // - Print the RETRIEVAL step header
        // - Print the query and search parameters (k, minScore)
        // - Build a SearchRequest with query, topK(k), similarityThreshold(minScore)
        // - Call vectorStore.similaritySearch(request)
        // - Stream results: extract getText(), print score if present, collect content into a list
        // - Return all collected parts joined with "\n\n"

        System.out.println("RETRIEVAL");
        System.out.printf("Query: %s, with the following search params: topK(%s), minScore(%s)%n", query, k, minScore);
        var searchRequest = SearchRequest.builder()
            .query(query)
            .topK(k)
            .similarityThreshold(minScore)
            .build();

        var results = vectorStore.similaritySearch(searchRequest);

        var context = new ArrayList<String>();
        results.forEach(document -> {
            var text = document.getText();
            var score = document.getScore();
            if (score != null) {
                System.out.printf("Document chunk added: %s \n with score: %.2f%n \n\n", text, score);
            }
            context.add(text);
        });

        return String.join("\n\n", context);
    }

    private String augmentPrompt(String query, String context) {
        //TODO:
        // - Print the AUGMENTATION step header
        // - Replace {context} and {query} placeholders in USER_PROMPT_TEMPLATE
        // - Print the resulting augmented prompt
        // - Return the formatted string
        System.out.println("AUGMENTATION");
        var augmentedPrompt = USER_PROMPT_TEMPLATE.replace("{context}", context).replace("{query}", query);
        System.out.println("AUGMENTED PROMPT: " + augmentedPrompt);
        return augmentedPrompt;
    }

    private String generateAnswer(String augmentedPrompt) {
        //TODO:
        // - Print the GENERATION step header
        // - Build ChatCompletionCreateParams with model, temperature(0.0), system message (SYSTEM_PROMPT), user message (augmentedPrompt)
        // - Call openAiClient.chat().completions().create(params)
        // - Extract the answer from choices[0].message().content()
        // - Print the answer
        // - Return the answer string
        System.out.println("GENERATION");
        var params = ChatCompletionCreateParams.builder()
            .model(Constants.GPT_5_4)
            .temperature(0.0)
            .addSystemMessage(SYSTEM_PROMPT)
            .addUserMessage(augmentedPrompt)
            .build();

        var result = openAiClient.chat().completions().create(params);
        var answer = result.choices().get(0).message().content().get();
        System.out.println("LLM RESPONSE:");
        System.out.println(answer);
        return answer;
    }

    public static void main(String[] args) {
        //TODO:
        // - Instantiate SpringRagApp passing an OpenAiEmbeddingModel built with OpenAiApi (apiKey), MetadataMode.EMBED,
        //   and OpenAiEmbeddingOptions (model="text-embedding-3-small") https://docs.spring.io/spring-ai/reference/api/embeddings/openai-embeddings.html#_manual_configuration
        // - Print a welcome message
        // - Create a Scanner and start an infinite loop reading user input
        // - Skip blank lines
        // - Step 1 (Retrieval):    call retrieveContext(query, 4, 0.3)
        // - Step 2 (Augmentation): call augmentPrompt(query, context)
        // - Step 3 (Generation):   call generateAnswer(augmented)

        SpringRagApp rag = new SpringRagApp(
            new OpenAiEmbeddingModel(
                OpenAiApi.builder()
                    .apiKey(Constants.OPENAI_API_KEY)
                    .build(),
                MetadataMode.EMBED,
                OpenAiEmbeddingOptions.builder()
                    .model("text-embedding-3-small")
                    .build()
            )
        );
        System.out.println("🎯 Microwave RAG Assistant (Spring AI)");

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("\n> ");
            System.out.flush();
            if (!scanner.hasNextLine()) break;
            String query = scanner.nextLine().strip();
            if (query.isEmpty()) continue;

            // Step 1: Retrieval
            String context = rag.retrieveContext(query, 4, 0.3); // play with k and minScore params
            // Step 2: Augmentation
            String augmented = rag.augmentPrompt(query, context);
            // Step 3: Generation
            rag.generateAnswer(augmented);
        }
    }
}
