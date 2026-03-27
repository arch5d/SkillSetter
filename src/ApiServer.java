import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ApiServer {
    private static final int DEFAULT_PORT = 8080;
    private static DatabaseManager dbManager;
    private static SkillManager skillManager;
    private static MatchEngine matchEngine;

    public static void main(String[] args) throws IOException {
        skillManager = new SkillManager();
        dbManager = new DatabaseManager(skillManager);
        matchEngine = new MatchEngine();
        
        int port = resolvePort(args);
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        
        server.createContext("/api/users", new UsersHandler());
        server.createContext("/api/user", new UserHandler());
        server.createContext("/api/register", new RegisterHandler());
        server.createContext("/api/matches", new MatchesHandler());
        server.createContext("/api/requests", new RequestsHandler());
        server.createContext("/api/deleteProfile", new DeleteProfileHandler());
        
        server.setExecutor(null);
        server.start();
        System.out.println("API Server is running on http://localhost:" + port);
        System.out.println("You can now open the frontend index.html file to use the app.");
    }

    private static int resolvePort(String[] args) {
        if (args != null && args.length > 0) {
            try {
                return Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {
                // Invalid CLI port, fallback to env/default.
            }
        }

        String envPort = System.getenv("PORT");
        if (envPort != null && !envPort.trim().isEmpty()) {
            try {
                return Integer.parseInt(envPort.trim());
            } catch (NumberFormatException ignored) {
                // Invalid env port, use default.
            }
        }

        return DEFAULT_PORT;
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
        exchange.getResponseHeaders().add("Content-Type", "application/json");

        if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        byte[] bytes = response.getBytes("UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> values = new HashMap<>();
        if (query == null || query.isEmpty()) {
            return values;
        }

        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                String key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
                String value = URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                values.put(key, value);
            }
        }
        return values;
    }

    private static String jsonString(String key, String json) {
        String regex = "\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"";
        Matcher matcher = Pattern.compile(regex).matcher(json);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    private static Integer jsonInt(String key, String json) {
        String regex = "\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*(-?\\d+)";
        Matcher matcher = Pattern.compile(regex).matcher(json);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return null;
    }

    private static List<Skill> parseSkills(String json) {
        List<Skill> parsed = new ArrayList<>();
        Matcher matcher = Pattern.compile("\\{\\s*\\\"name\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"\\s*,\\s*\\\"level\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"\\s*\\}").matcher(json);
        while (matcher.find()) {
            try {
                String skillName = matcher.group(1).trim();
                Skill.SkillLevel level = Skill.SkillLevel.valueOf(matcher.group(2).trim().toUpperCase());
                parsed.add(skillManager.createSkill(skillName, level));
            } catch (IllegalArgumentException ignored) {
                // Invalid skill level in payload: skip that skill and continue.
            }
        }
        return parsed;
    }

    static class UsersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) { sendResponse(exchange, 204, ""); return; }
            List<User> users = dbManager.loadUsers();
            String json = JSONHelper.toJSON(users);
            sendResponse(exchange, 200, json);
        }
    }

    static class UserHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                sendResponse(exchange, 204, "");
                return;
            }
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            Map<String, String> query = parseQuery(exchange.getRequestURI().getQuery());
            String email = query.get("email");
            if (email == null || email.trim().isEmpty()) {
                sendResponse(exchange, 400, "{\"error\":\"Missing email\"}");
                return;
            }

            User user = dbManager.getUserByEmail(email);
            if (user == null) {
                sendResponse(exchange, 404, "{\"error\":\"User not found\"}");
                return;
            }

            sendResponse(exchange, 200, JSONHelper.toJSON(user));
        }
    }

    static class RegisterHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) { sendResponse(exchange, 204, ""); return; }

            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                try {
                    InputStream is = exchange.getRequestBody();
                    String body = new String(is.readAllBytes(), "UTF-8");
                    String name = jsonString("name", body);
                    String email = jsonString("email", body);
                    Integer availability = jsonInt("availability", body);
                    String roleRaw = jsonString("role", body);
                    String goalRaw = jsonString("goal", body);
                    String modeRaw = jsonString("mode", body);
                    Integer teamSize = jsonInt("teamSize", body);
                    List<Skill> userSkills = parseSkills(body);

                    if (name.isEmpty() || email.isEmpty() || availability == null || roleRaw.isEmpty() || goalRaw.isEmpty() || modeRaw.isEmpty()) {
                        sendResponse(exchange, 400, "{\"error\":\"Missing required fields\"}");
                        return;
                    }

                    User.Role role = User.Role.valueOf(roleRaw.toUpperCase());
                    User.Goal goal = User.Goal.valueOf(goalRaw.toUpperCase());
                    User.Mode mode = User.Mode.valueOf(modeRaw.toUpperCase());
                    if (role != User.Role.LEADER) {
                        teamSize = null;
                    }

                    User newUser = new User(name, email, userSkills, availability, role, goal, mode, teamSize);
                    dbManager.saveUser(newUser);

                    sendResponse(exchange, 200, "{\"status\":\"success\"}");
                } catch (Exception e) {
                    e.printStackTrace();
                    sendResponse(exchange, 400, "{\"error\":\"" + e.getMessage() + "\"}");
                }
            } else {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
            }
        }
    }

    static class MatchesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) { sendResponse(exchange, 204, ""); return; }

            Map<String, String> query = parseQuery(exchange.getRequestURI().getQuery());
            String email = query.get("email");
            if (email != null && !email.trim().isEmpty()) {
                List<User> users = dbManager.loadUsers();
                User target = users.stream().filter(u -> u.getEmail().equalsIgnoreCase(email)).findFirst().orElse(null);
                
                if (target != null) {
                    List<MatchEngine.MatchResult> matches = matchEngine.findMatches(target, users);
                    sendResponse(exchange, 200, JSONHelper.matchResultsToJSON(matches));
                    return;
                }
            }
            sendResponse(exchange, 404, "{\"error\":\"User not found\"}");
        }
    }

    static class RequestsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) { sendResponse(exchange, 204, ""); return; }

            String method = exchange.getRequestMethod();
            if ("GET".equalsIgnoreCase(method)) {
                Map<String, String> query = parseQuery(exchange.getRequestURI().getQuery());
                String email = query.get("email");
                if (email != null && !email.trim().isEmpty()) {
                    List<ConnectionRequest> reqs = dbManager.getIncomingRequests(email);
                    sendResponse(exchange, 200, JSONHelper.connectionRequestsToJSON(reqs));
                    return;
                }
            } else if ("POST".equalsIgnoreCase(method)) {
                InputStream is = exchange.getRequestBody();
                String body = new String(is.readAllBytes(), "UTF-8");
                String senderEmail = jsonString("sender", body);
                String receiverEmail = jsonString("receiver", body);
                if (!senderEmail.isEmpty() && !receiverEmail.isEmpty()) {
                    boolean created = dbManager.sendRequest(senderEmail, receiverEmail);
                    if (!created) {
                        sendResponse(exchange, 409, "{\"error\":\"Request already exists or is invalid\"}");
                        return;
                    }
                    sendResponse(exchange, 200, "{\"status\":\"success\"}");
                    return;
                }
            } else if ("PUT".equalsIgnoreCase(method)) {
                InputStream is = exchange.getRequestBody();
                String body = new String(is.readAllBytes(), "UTF-8");
                Integer requestId = jsonInt("id", body);
                String status = jsonString("status", body);
                String actorEmail = jsonString("actor", body);
                if (requestId != null && !status.isEmpty() && !actorEmail.isEmpty()) {
                    boolean updated = dbManager.updateRequestStatus(requestId, actorEmail, status);
                    if (!updated) {
                        sendResponse(exchange, 403, "{\"error\":\"Only the receiver can update a pending request\"}");
                        return;
                    }
                    sendResponse(exchange, 200, "{\"status\":\"success\"}");
                    return;
                }
            }
            sendResponse(exchange, 400, "{\"error\":\"Bad Request\"}");
        }
    }

    static class DeleteProfileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) { sendResponse(exchange, 204, ""); return; }

            if ("DELETE".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> query = parseQuery(exchange.getRequestURI().getQuery());
                String email = query.get("email");
                if (email != null && !email.trim().isEmpty()) {
                    dbManager.deleteUser(email);
                    sendResponse(exchange, 200, "{\"status\":\"success\"}");
                    return;
                }
            }
            sendResponse(exchange, 400, "{\"error\":\"Bad Request\"}");
        }
    }
}
