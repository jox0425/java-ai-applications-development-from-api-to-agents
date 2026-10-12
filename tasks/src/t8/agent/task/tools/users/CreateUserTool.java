package t8.agent.task.tools.users;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import commons.exceptions.TaskNotImplementedException;
import commons.user.service.UserCreate;
import commons.user.service.UserServiceClient;

public class CreateUserTool extends BaseUserServiceTool {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public CreateUserTool(UserServiceClient userClient) {
        super(userClient);
    }

    @Override
    public String getName() {
        //TODO: Return tool name "add_user"
        return "add_user";
    }

    @Override
    public String getDescription() {
        //TODO: Return a short description of what this tool does
        return "Adds a new user to the User Service.";
    }

    @Override
    public String getInputSchema() {
        return """
            {
              "type": "object",
              "required": ["name", "surname", "email", "about_me"],
              "properties": {
                "name": { "type": "string" },
                "surname": { "type": "string" },
                "email": { "type": "string" },
                "about_me": { "type": "string" },
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
            """;
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        //TODO:
        // - Convert `arguments` to `UserCreate` via `objectMapper.convertValue(arguments, UserCreate.class)`
        // - Call `userClient.addUser(user)` and return the result
        // - Wrap in try-catch and return error string on exception
        var userCreate = objectMapper.convertValue(arguments, UserCreate.class);
        try {
            return userClient.addUser(userCreate);
        } catch (Exception e) {
            return "Error adding user: " + e.getMessage();
        }
    }
}
