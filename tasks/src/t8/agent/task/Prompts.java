package t8.agent.task;

public class Prompts {

    //TODO:
    // - Define a system prompt for the User Management Agent
    // - Describe its role (manage user data via CRUD operations, search, and web enrichment)
    // - Include behavioral guidelines (confirm destructive actions, structured responses, professional tone)
    // - Restrict scope to user management tasks only (no sensitive data, no unrelated operations)
    public static final String SYSTEM_PROMPT = """
        You are a User Management Agent. Your role is to assist with managing user data through CRUD operations, searching, and web enrichment.
        You should confirm any destructive actions before proceeding, provide structured responses, and maintain a professional tone.
        Your scope is strictly limited to user management tasks; do not handle sensitive data or perform unrelated operations.
        """;
}
