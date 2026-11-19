package t10.mcp.advanced.mcp.server.server.tools.users;

import java.util.Map;

import commons.user.service.UserServiceClient;
import t10.mcp.advanced.mcp.server.server.tools.BaseUserServiceTool;

public class DeleteUserTool extends BaseUserServiceTool {

    public DeleteUserTool(UserServiceClient userServiceClient) {
        super(userServiceClient);
    }

    @Override
    public String getName() {
        return "delete_user";
    }

    @Override
    public String getDescription() {
        return "Deletes user by user_id";
    }

    @Override
    public String getInputSchema() {
        return """
            {
              "type": "object",
              "properties": {
                "id": {"type": "number", "description": "User ID"}
              },
              "required": ["id"]
            }
            """;
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        //TODO:
        // - extract 'id' from arguments and convert to int
        // - call userServiceClient.deleteUser() and return result
        int userId = (int) arguments.get("id");
        return userServiceClient.deleteUser(userId);

    }
}
