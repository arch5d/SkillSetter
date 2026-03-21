import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Main console application and in-memory data store.
 */
public class SkillSetterApp {
    private final List<User> users;
    private final List<ConnectionRequest> requests;
    private final MatchEngine matchEngine;

    public SkillSetterApp() {
        users = new ArrayList<>();
        requests = new ArrayList<>();
        matchEngine = new MatchEngine();
        seedSampleUsers();
    }

    public static void main(String[] args) {
        SkillSetterApp app = new SkillSetterApp();
        if (args.length > 0 && "ui".equalsIgnoreCase(args[0])) {
            javax.swing.SwingUtilities.invokeLater(() -> new SkillSetterUI(app).show());
        } else {
            app.runConsole();
        }
    }

    public void runConsole() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Welcome to SkillSetter");

        boolean running = true;
        while (running) {
            printMenu();
            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1":
                    registerUserFromConsole(scanner);
                    break;
                case "2":
                    viewMatchesFromConsole(scanner);
                    break;
                case "3":
                    sendRequestFromConsole(scanner);
                    break;
                case "4":
                    respondToRequestsFromConsole(scanner);
                    break;
                case "5":
                    running = false;
                    System.out.println("Goodbye!");
                    break;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    private void printMenu() {
        System.out.println();
        System.out.println("1. Register User");
        System.out.println("2. View Matches");
        System.out.println("3. Send Request");
        System.out.println("4. Respond to Requests");
        System.out.println("5. Exit");
        System.out.print("Choose an option: ");
    }

    public User registerUser(String name, String email, List<String> skills,
                             User.SkillLevel skillLevel, int availability,
                             User.Role role, User.Goal goal, User.Mode mode) {
        User user = new User(name, email, skills, skillLevel, availability, role, goal, mode);
        users.add(user);
        return user;
    }

    private void registerUserFromConsole(Scanner scanner) {
        System.out.print("Name: ");
        String name = scanner.nextLine();
        System.out.print("Email: ");
        String email = scanner.nextLine();
        System.out.print("Skills (comma separated): ");
        List<String> skills = parseSkills(scanner.nextLine());
        User.SkillLevel skillLevel = promptSkillLevel(scanner);
        int availability = promptInt(scanner, "Availability hours per week: ");
        User.Role role = promptEnum(scanner, User.Role.values(), "Role");
        User.Goal goal = promptEnum(scanner, User.Goal.values(), "Goal");
        User.Mode mode = promptEnum(scanner, User.Mode.values(), "Mode");

        User user = registerUser(name, email, skills, skillLevel, availability, role, goal, mode);
        System.out.println("Registered: " + user.getSummary());
    }

    private void viewMatchesFromConsole(Scanner scanner) {
        User currentUser = chooseUser(scanner, "Select user to view matches");
        if (currentUser == null) {
            return;
        }

        List<MatchEngine.MatchResult> matches = getMatchesForUser(currentUser);
        if (matches.isEmpty()) {
            System.out.println("No matches available.");
            return;
        }

        System.out.println("Matches for " + currentUser.getName() + ":");
        for (MatchEngine.MatchResult match : matches) {
            System.out.println(match.toDisplayString());
        }
    }

    private void sendRequestFromConsole(Scanner scanner) {
        User sender = chooseUser(scanner, "Select sender");
        User receiver = chooseUser(scanner, "Select receiver");
        if (sender == null || receiver == null) {
            return;
        }
        if (sender.equals(receiver)) {
            System.out.println("Cannot send a request to the same user.");
            return;
        }

        ConnectionRequest request = sendRequest(sender, receiver);
        System.out.println("Request sent: " + request.getSummaryFor(sender));
    }

    private void respondToRequestsFromConsole(Scanner scanner) {
        User receiver = chooseUser(scanner, "Select user to manage requests");
        if (receiver == null) {
            return;
        }

        List<ConnectionRequest> pendingRequests = getPendingRequestsFor(receiver);
        if (pendingRequests.isEmpty()) {
            System.out.println("No pending requests.");
            return;
        }

        for (int i = 0; i < pendingRequests.size(); i++) {
            ConnectionRequest request = pendingRequests.get(i);
            System.out.printf("%d. %s%n", i + 1, request.getSummaryFor(receiver));
        }

        int requestChoice = promptInt(scanner, "Choose request number: ") - 1;
        if (requestChoice < 0 || requestChoice >= pendingRequests.size()) {
            System.out.println("Invalid request number.");
            return;
        }

        System.out.print("Accept or Reject (A/R): ");
        String decision = scanner.nextLine().trim();
        if (decision.equalsIgnoreCase("A")) {
            respondToRequest(pendingRequests.get(requestChoice), true);
            System.out.println("Request accepted.");
        } else if (decision.equalsIgnoreCase("R")) {
            respondToRequest(pendingRequests.get(requestChoice), false);
            System.out.println("Request rejected.");
        } else {
            System.out.println("Unknown option.");
        }
    }

    public List<User> getUsers() {
        return users;
    }

    public List<ConnectionRequest> getRequests() {
        return requests;
    }

    public List<MatchEngine.MatchResult> getMatchesForUser(User user) {
        return matchEngine.findMatches(user, users);
    }

    public ConnectionRequest sendRequest(User sender, User receiver) {
        ConnectionRequest request = new ConnectionRequest(sender, receiver);
        requests.add(request);
        return request;
    }

    public void respondToRequest(ConnectionRequest request, boolean accept) {
        if (accept) {
            request.accept();
        } else {
            request.reject();
        }
    }

    public List<ConnectionRequest> getPendingRequestsFor(User receiver) {
        List<ConnectionRequest> pending = new ArrayList<>();
        for (ConnectionRequest request : requests) {
            if (request.isPendingFor(receiver)) {
                pending.add(request);
            }
        }
        return pending;
    }

    public static List<String> parseSkills(String rawSkills) {
        List<String> skills = new ArrayList<>();
        for (String skill : rawSkills.split(",")) {
            String trimmed = skill.trim();
            if (!trimmed.isEmpty()) {
                skills.add(trimmed);
            }
        }
        return skills;
    }

    private User chooseUser(Scanner scanner, String prompt) {
        if (users.isEmpty()) {
            System.out.println("No users registered yet.");
            return null;
        }

        System.out.println(prompt + ":");
        for (int i = 0; i < users.size(); i++) {
            System.out.printf("%d. %s%n", i + 1, users.get(i).getSummary());
        }

        int userChoice = promptInt(scanner, "Choose user number: ") - 1;
        if (userChoice < 0 || userChoice >= users.size()) {
            System.out.println("Invalid user number.");
            return null;
        }
        return users.get(userChoice);
    }

    private User.SkillLevel promptSkillLevel(Scanner scanner) {
        return promptEnum(scanner, User.SkillLevel.values(), "Skill level");
    }

    private <T extends Enum<T>> T promptEnum(Scanner scanner, T[] values, String label) {
        System.out.println(label + " options:");
        for (int i = 0; i < values.length; i++) {
            System.out.printf("%d. %s%n", i + 1, values[i]);
        }

        while (true) {
            int choice = promptInt(scanner, "Choose " + label + " number: ") - 1;
            if (choice >= 0 && choice < values.length) {
                return values[choice];
            }
            System.out.println("Invalid choice. Try again.");
        }
    }

    private int promptInt(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine();
            try {
                return Integer.parseInt(input.trim());
            } catch (NumberFormatException exception) {
                System.out.println("Please enter a valid number.");
            }
        }
    }

    private void seedSampleUsers() {
        registerUser("Ava", "ava@example.com", parseSkills("Java,UI,Presentation"),
                User.SkillLevel.INTERMEDIATE, 8, User.Role.LEADER, User.Goal.HACKATHON, User.Mode.BUILD);
        registerUser("Ben", "ben@example.com", parseSkills("Python,Testing,Research"),
                User.SkillLevel.BEGINNER, 10, User.Role.LEARNER, User.Goal.HACKATHON, User.Mode.JOIN);
        registerUser("Cara", "cara@example.com", parseSkills("Design,Marketing,Pitching"),
                User.SkillLevel.ADVANCED, 7, User.Role.TEAMMATE, User.Goal.STARTUP, User.Mode.BUILD);
    }
}
