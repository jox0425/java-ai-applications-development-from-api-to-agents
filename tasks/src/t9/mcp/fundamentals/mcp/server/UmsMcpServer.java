package t9.mcp.fundamentals.mcp.server;

import commons.exceptions.TaskNotImplementedException;
import commons.user.service.UserCreate;
import commons.user.service.UserSearchRequest;
import commons.user.service.UserServiceClient;
import commons.user.service.UserUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpPrompt;
import org.springaicommunity.mcp.annotation.McpResource;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

// https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html
// https://docs.spring.io/spring-ai/reference/api/mcp/mcp-annotations-server.html
@Component
public class UmsMcpServer {

    private static final Logger log = LoggerFactory.getLogger(UmsMcpServer.class);
    private final UserServiceClient userServiceClient = new UserServiceClient();

    //TODO:
    // - define SEARCH_ASSISTANT_PROMPT: list available params (name, surname, email, gender),
    //   describe search strategy, effective combinations, and useful tips
    private static final String SEARCH_ASSISTANT_PROMPT = """
                Help users formulate effective searches for user profiles.
        
                Available search parameters:
                - name
                - surname
                - email
                - gender
        
                Search strategy:
                - Use the parameters the user provides; leave unspecified fields empty.
                - Combine fields to narrow results. Name and surname work well together;
                  email is useful for a more targeted search.
                - If a search is too narrow or returns no results, try fewer parameters
                  or check the spelling and formatting of the supplied values.
                - Ask the user for clarification when their request does not provide
                  enough information to form a useful search.
        """;

    //TODO:
    // - define PROFILE_CREATION_PROMPT: list required vs optional fields,
    //   include address/credit card guidelines and a diversity note
    private static final String PROFILE_CREATION_PROMPT = """
                Guide users in creating realistic user profiles.
        
                Required fields:
                - name
                - surname
                - email
                - gender
        
                Optional fields:
                - address
                - phone number
                - date of birth
        
                Guidelines:
                - Ensure that the email is valid and follows standard formatting.
                - For addresses, use realistic street names, cities, and postal codes.
                - Credit card information should be formatted correctly but should not be real.
        
                Diversity note:
                - Encourage users to create profiles that reflect a variety of backgrounds,
                  cultures, and experiences. Avoid stereotypes and ensure inclusivity in profile creation.
        """;

    // ==================== TOOL DEFINITIONS ====================

    @McpTool(name = "get_user_by_id", description = "Provides full user information by id")
    public String getUserById(@McpToolParam int userId) {
        //TODO:
        // - log invocation with userId
        // - call UserServiceClient().getUser(userId), log completion, and return the result
        // - catch Exception: log error and return "Error: " + e.getMessage()
        log.info("Invoked getUserById with userId: {}", userId);
        try {
            String result = userServiceClient.getUser(userId);
            log.info("Successfully retrieved user information for userId: {}", userId);
            return result;
        } catch (Exception e) {
            log.error("Error retrieving user information for userId: {}: {}", userId, e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @McpTool(name = "delete_user", description = "Deletes user")
    public String deleteUser(@McpToolParam int userId) {
        //TODO:
        // - log invocation with userId
        // - call UserServiceClient().deleteUser(userId), log completion, and return the result
        // - catch Exception: log error and return "Error: " + e.getMessage()
        log.info("Invoked deleteUser with userId: {}", userId);
        try {
            String result = userServiceClient.deleteUser(userId);
            log.info("Successfully deleted user with userId: {}", userId);
            return result;
        } catch (Exception e) {
            log.error("Error deleting user with userId: {}: {}", userId, e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @McpTool(name = "search_user", description = "Searches for users by name, surname, email and gender")
    public String searchUser(@McpToolParam UserSearchRequest userSearchRequest) {
        //TODO:
        // - log invocation with userSearchRequest
        // - call UserServiceClient().searchUsers(name, surname, email, gender) using fields from userSearchRequest, log completion, and return the result
        // - catch Exception: log error and return "Error: " + e.getMessage()
        log.info("Invoked searchUser with search request: {}", userSearchRequest);
        try {
            String result = userServiceClient.searchUsers(
                    userSearchRequest.name(),
                    userSearchRequest.surname(),
                    userSearchRequest.email(),
                    userSearchRequest.gender()
            );
            log.info("Successfully completed user search with request: {}", userSearchRequest);
            return result;
        } catch (Exception e) {
            log.error("Error searching users with request: {}: {}", userSearchRequest, e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @McpTool(name = "add_user", description = "Adds new user into the system")
    public String addUser(@McpToolParam UserCreate userCreate) {
        //TODO:
        // - log invocation with userCreate.email()
        // - call UserServiceClient().addUser(userCreate), log completion, and return the result
        // - catch Exception: log error and return "Error: " + e.getMessage()
        log.info("Invoked addUser with email: {}", userCreate.email());
        try {
            String result = userServiceClient.addUser(userCreate);
            log.info("Successfully added new user with email: {}", userCreate.email());
            return result;
        } catch (Exception e) {
            log.error("Error adding new user with email: {}: {}", userCreate.email(), e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @McpTool(name = "update_user", description = "Updates user by userId")
    public String updateUser(@McpToolParam int userId, @McpToolParam UserUpdate userUpdate) {
        //TODO:
        // - log invocation with userId
        // - call UserServiceClient().updateUser(userId, userUpdate), log completion, and return the result
        // - catch Exception: log error and return "Error: " + e.getMessage()
        log.info("Invoked updateUser with userId: {}", userId);
        try {
            String result = userServiceClient.updateUser(userId, userUpdate);
            log.info("Successfully updated user with userId: {}", userId);
            return result;
        } catch (Exception e) {
            log.error("Error updating user with userId: {}: {}", userId, e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    // ==================== MCP RESOURCES ====================

    @McpResource(
            uri = "users-management://flow-diagram",
            name = "flow-diagram",
            mimeType = "image/png",
            description = "The Users Management Service flow diagram as PNG image"
    )
    public String flowDiagramResource() {
        //TODO:
        // - read flow.png via Files.readAllBytes(Path.of("tasks/src/t9/mcp/fundamentals/flow.png"))
        // - return Base64.getEncoder().encodeToString(bytes)
        // - wrap IOException in RuntimeException
        try {
            byte[] bytes = Files.readAllBytes(Path.of("tasks/src/t9/mcp/fundamentals/flow.png"));
            return Base64.getEncoder().encodeToString(bytes);
        } catch (IOException e) {
            throw new RuntimeException("Error reading flow diagram resource", e);
        }
    }

    // ==================== MCP PROMPTS ====================

    @McpPrompt(description = "users formulate effective search queries")
    public String searchAssistantPrompt() {
        //TODO:
        // - return SEARCH_ASSISTANT_PROMPT
        return SEARCH_ASSISTANT_PROMPT;
    }

    @McpPrompt(description = "Guides creation of realistic user profiles")
    public String profileCreationPrompt() {
        //TODO:
        // - return PROFILE_CREATION_PROMPT
        return PROFILE_CREATION_PROMPT;
    }

}
