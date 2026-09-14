package t5.rag.advanced;

import java.util.Scanner;

import commons.Constants;
import commons.model.Conversation;
import commons.model.Message;
import commons.model.Role;
import t5.rag.advanced.chat.ChatCompletionClient;
import t5.rag.advanced.embeddings.EmbeddingsClient;
import t5.rag.advanced.embeddings.SearchMode;
import t5.rag.advanced.embeddings.TextProcessor;

public class App {

    private static final String MANUAL_PATH = "tasks/src/t5/rag/advanced/microwave_manual.txt";

    //TODO:
    // - describe the assistant as a RAG-powered assistant for microwave usage questions
    // - explain user message structure: RAG CONTEXT section (retrieved documents) and USER QUESTION section
    // - instruct LLM to: answer using RAG CONTEXT, cite sources, restrict answers to context/history,
    //   refuse questions unrelated to microwave usage or outside context scope
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

    //TODO:
    // - define structured template with ##RAG CONTEXT section and ##USER QUESTION section
    // - use {context} and {query} placeholders for runtime substitution
    private static final String USER_PROMPT_TEMPLATE = """
        ##USER QUESTION
        %s
        
        ##RAG CONTEXT
        %s
        """;

    public static void main(String[] args) {
        //TODO:
        // - instantiate EmbeddingsClient: "text-embedding-3-small", Constants.OPENAI_EMBEDDINGS_ENDPOINT, Constants.OPENAI_API_KEY
        // - instantiate ChatCompletionClient: Constants.GPT_5_4, Constants.OPENAI_CHAT_COMPLETIONS_ENDPOINT, Constants.OPENAI_API_KEY
        // - instantiate TextProcessor: localhost, port 5433, database "vectordb", user "postgres", password "postgres"
        // - prompt user (y/n) to load context; on yes call textProcessor.processTextFile(MANUAL_PATH, 400, 40, 384)
        // - create Conversation, add SYSTEM message with SYSTEM_PROMPT
        // - run while loop:
        //   - read user input from console; exit on "quit"/"exit" or end of stream
        //   - STEP 1 RETRIEVAL: textProcessor.search(SearchMode.EUCLIDEAN_DISTANCE, userRequest, 5, 0.01, 384)
        //   - STEP 2 AUGMENTATION: fill USER_PROMPT_TEMPLATE {context} and {query}, add as USER message to conversation
        //   - STEP 3 GENERATION: completionClient.getCompletion(conversation.getMessages()), print and add response to conversation
        // NOTE: start docker-compose.yml before running to bring up Postgres + pgvector on port 5433

        var embeddingsClient = new EmbeddingsClient(Constants.OPENAI_EMBEDDINGS_ENDPOINT, "text-embedding-3-small",
            Constants.OPENAI_API_KEY);
        var chatCompletionClient = new ChatCompletionClient(Constants.OPENAI_CHAT_COMPLETIONS_ENDPOINT,
            Constants.GPT_5_4, Constants.OPENAI_API_KEY);
        var textProcessor = new TextProcessor(embeddingsClient, "localhost", 5433, "vectordb", "postgres", "postgres");

        var scanner = new Scanner(System.in);

        System.out.println("Load context? Y/N");
        var loadContext = scanner.nextLine();
        if ("y".equalsIgnoreCase(loadContext)) {
            textProcessor.processTextFile(MANUAL_PATH, 400, 40, 384);
        }
        Conversation conversation = new Conversation();
        var systemMessage = new Message(Role.SYSTEM, SYSTEM_PROMPT);
        conversation.addMessage(systemMessage);

        System.out.println("Type quit or exit to exit from the chat.");
        while (true) {
            var userRequest = scanner.nextLine();
            if ("quit".equalsIgnoreCase(userRequest) || "exit".equalsIgnoreCase(userRequest)) {
                break;
            }

            var context = textProcessor.search(SearchMode.EUCLIDEAN_DISTANCE, userRequest, 5, 0.01, 384);
            var augmentedMessage = String.format(USER_PROMPT_TEMPLATE, userRequest, context);
            var userMessage = new Message(Role.USER, augmentedMessage);
            conversation.addMessage(userMessage);

            var responseMessage = chatCompletionClient.getCompletion(conversation.getMessages());
            System.out.println(responseMessage);
            conversation.addMessage(responseMessage);
        }

    }
}
