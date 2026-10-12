package t8.agent.task.tools.users;

import java.util.Map;

import commons.exceptions.TaskNotImplementedException;
import commons.user.service.UserServiceClient;

public class GetUserByIdTool extends BaseUserServiceTool {

    public GetUserByIdTool(UserServiceClient userClient) {
        super(userClient);
    }

    @Override
    public String getName() {
        //TODO: Return tool name "get_user_by_id"
        return "get_user_by_id";
    }

    @Override
    public String getDescription() {
        //TODO: Return a short description of what this tool does
        return "Fetches user information by user ID from the User Service.";
    }

    @Override
    public String getInputSchema() {
        //TODO: Return JSON schema — accepts user `id` (number) as a required parameter
        return """
            {
              "type": "object",
              "properties": {
                "id": {
                  "type": "number",
                  "description": "User ID"
                }
              },
              "required": [
                "id"
              ]
            }
            """;
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        //TODO:
        // - Extract `id` as int from arguments (cast via Number)
        // - Call `userClient.getUser(id)` and return the result
        // - Wrap in try-catch and return error string on exception
        var id = (Number) arguments.get("id");
        try {
            return userClient.getUser(id.intValue());
        } catch (Exception e) {
            return "Error fetching user: " + e.getMessage();
        }
    }
}
