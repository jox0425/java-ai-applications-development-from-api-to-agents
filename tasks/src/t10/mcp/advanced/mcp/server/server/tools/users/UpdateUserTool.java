package t10.mcp.advanced.mcp.server.server.tools.users;

import commons.user.service.UserServiceClient;
import commons.user.service.UserUpdate;
import commons.exceptions.TaskNotImplementedException;
import t10.mcp.advanced.mcp.server.server.tools.BaseUserServiceTool;

import java.util.Map;

public class UpdateUserTool extends BaseUserServiceTool {

    public UpdateUserTool(UserServiceClient userServiceClient) {
        super(userServiceClient);
    }

    @Override
    public String getName() {
        return "update_user";
    }

    @Override
    public String getDescription() {
        return "Updates user info";
    }

    @Override
    public String getInputSchema() {
        return """
                {
                  "type": "object",
                  "properties": {
                    "id": {"type": "number", "description": "User ID that should be updated."},
                    "new_info": {
                      "type": "object",
                      "properties": {
                        "name":          {"type": "string"},
                        "surname":       {"type": "string"},
                        "email":         {"type": "string"},
                        "phone":         {"type": "string"},
                        "date_of_birth": {"type": "string"},
                        "address": {
                          "type": "object",
                          "properties": {
                            "country":    {"type": "string"},
                            "city":       {"type": "string"},
                            "street":     {"type": "string"},
                            "flat_house": {"type": "string"}
                          }
                        },
                        "gender":  {"type": "string"},
                        "company": {"type": "string"},
                        "salary":  {"type": "number"},
                        "credit_card": {
                          "type": "object",
                          "properties": {
                            "num":      {"type": "string"},
                            "cvv":      {"type": "string"},
                            "exp_date": {"type": "string"}
                          }
                        }
                      }
                    }
                  },
                  "required": ["id"]
                }
                """;
    }

    @Override
    @SuppressWarnings("unchecked")
    public String execute(Map<String, Object> arguments) {
        //TODO:
        // - extract 'id' and 'new_info' map from arguments
        // - convert 'new_info' map to UserUpdate object using objectMapper
        // - call userServiceClient.updateUser() and return result
        int userId = (int) arguments.get("id");
        Map<String, Object> newInfoMap = (Map<String, Object>) arguments.get("new_info");
        UserUpdate userUpdate = objectMapper.convertValue(newInfoMap, UserUpdate.class);
        return userServiceClient.updateUser(userId, userUpdate);
    }
}
