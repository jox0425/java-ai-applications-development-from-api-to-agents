package t8.agent.task.tools.users;

import java.util.Map;

import commons.exceptions.TaskNotImplementedException;
import commons.user.service.UserServiceClient;

public class SearchUsersTool extends BaseUserServiceTool {

    public SearchUsersTool(UserServiceClient userClient) {
        super(userClient);
    }

    @Override
    public String getName() {
        //TODO: Return tool name "search_users"
        return "search_users";
    }

    @Override
    public String getDescription() {
        //TODO: Return a short description of what this tool does
        return "Searches for users in the User Service based on optional criteria.";
    }

    @Override
    public String getInputSchema() {
        //TODO: Return JSON schema — optional string properties: name, surname, email, gender (none required)
        return """
            {
              "type": "object",
              "properties": {
                "name": { "type": ["string", "null"] },
                "surname": { "type": ["string", "null"] },
                "email": { "type": ["string", "null"] },
                "gender": {
                  "type": ["string", "null"],
                  "enum": ["male", "female", null]
                }
              }
            }
            """;
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        //TODO:
        // - Extract optional name, surname, email, gender strings from arguments (may be null)
        // - Call `userClient.searchUsers(name, surname, email, gender)` and return the result
        // - Wrap in try-catch and return error string on exception
        var name = (String) arguments.get("name");
        var surname = (String) arguments.get("surname");
        var email = (String) arguments.get("email");
        var gender = (String) arguments.get("gender");
        try {
            return userClient.searchUsers(name, surname, email, gender);
        } catch (Exception e) {
            return "Error searching users: " + e.getMessage();
        }
    }
}
