package t8.agent.task.tools.users;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import commons.exceptions.TaskNotImplementedException;
import commons.user.service.UserServiceClient;
import commons.user.service.UserUpdate;

public class UpdateUserTool extends BaseUserServiceTool {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public UpdateUserTool(UserServiceClient userClient) {
        super(userClient);
    }

    @Override
    public String getName() {
        //TODO: Return tool name "update_user"
        return "update_user";
    }

    @Override
    public String getDescription() {
        //TODO: Return a short description of what this tool does
        return "Updates user information in the User Service by user ID.";
    }

    @Override
    public String getInputSchema() {
        //TODO: Return JSON schema — required: id (number, user ID to update);
        //   optional new_info object with updatable user fields (name, surname, email, phone,
        //   date_of_birth, address, gender, company, salary, credit_card)
        return """
            {
              "type": "object",
              "required": ["id"],
              "properties": {
                "id": { "type": "number", "description": "User ID to update" },
                "new_info": {
                  "type": ["object", "null"],
                  "description": "New user information to update",
                  "properties": {
                    "name": { "type": ["string", "null"] },
                    "surname": { "type": ["string", "null"] },
                    "email": { "type": ["string", "null"] },
                    "phone": { "type": ["string", "null"] },
                    "date_of_birth": { "type": ["string", "null"] },
                    "address": {
                      "type": ["object", "null"],
                      "properties": {
                        "country": { "type": ["string", "null"] },
                        "city": { "type": ["string", "null"] },
                        "street": { "type": ["string", "null"] },
                        "flat_house": { "type": ["string", "null"] }
                      }
                    },
                    "gender": {
                      "type": ["string", "null"],
                      "enum": ["male", "female", null]
                    },
                    "company": { "type": ["string", "null"] },
                    "salary": { "type": ["number", "null"] },
                    "credit_card": {
                        "type": ["object", "null"],
                        "properties": {
                            "num": { "type": ["string", "null"] },
                            "cvv": { "type": ["string", "null"] },
                            "exp_date": { "type": ["string", "null"] }
                        }
                    }
                  }
                }
              }
            }
            """;
    }

    @Override
    @SuppressWarnings("unchecked")
    public String execute(Map<String, Object> arguments) {
        //TODO:
        // - Extract `id` as int from arguments (cast via Number)
        // - Extract `new_info` as Map from arguments
        // - Convert `new_info` to `UserUpdate` via `objectMapper.convertValue(newInfo, UserUpdate.class)`
        // - Call `userClient.updateUser(userId, user)` and return the result
        // - Wrap in try-catch and return error string on exception
        var id = (Number) arguments.get("id");
        var newInfo = (Map<String, Object>) arguments.get("new_info");
        try {
            var userUpdate = objectMapper.convertValue(newInfo, UserUpdate.class);
            return userClient.updateUser(id.intValue(), userUpdate);
        } catch (Exception e) {
            return "Error updating user: " + e.getMessage();
        }
    }
}
