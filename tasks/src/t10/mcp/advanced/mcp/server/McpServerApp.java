package t10.mcp.advanced.mcp.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Map;

@SpringBootApplication
public class McpServerApp {

    public static void main(String[] args) {
        //TODO:
        // - initialize SpringApplication with McpServerApp class
        // - set default properties:
        //      - server.port (8006)
        //      - logging.level.t10.mcp.advanced=DEBUG
        //      - spring.autoconfigure.exclude
        //      - org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
        //      - org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration
        // - run the application
        SpringApplication app = new SpringApplication(McpServerApp.class);
        app.setDefaultProperties(Map.of(
                "server.port", "8006",
                "logging.level.t10.mcp.advanced", "DEBUG",
                "spring.autoconfigure.exclude", "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration,org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration"
        ));
        app.run(args);
    }
}
