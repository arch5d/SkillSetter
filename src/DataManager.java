import java.io.*;
import java.util.*;

//Handles data persistence for users.
public class DataManager {
    private static final String USERS_FILE = "users.txt";
    private final SkillManager skillManager;

    public DataManager(SkillManager skillManager) {
        this.skillManager = skillManager;
    }

    public void saveUsers(List<User> users) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(USERS_FILE))) {
            for (User user : users) {
                writer.println(userToString(user));
            }
        } catch (IOException e) {
            System.err.println("Error saving users: " + e.getMessage());
        }
    }

    public List<User> loadUsers() {
        List<User> users = new ArrayList<>();
        File file = new File(USERS_FILE);
        if (!file.exists()) {
            return users;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                User user = stringToUser(line);
                if (user != null) {
                    users.add(user);
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading users: " + e.getMessage());
        }
        return users;
    }

    private String userToString(User user) {
        StringBuilder sb = new StringBuilder();
        sb.append(user.getName()).append("|");
        sb.append(user.getEmail()).append("|");

        // Skills
        for (int i = 0; i < user.getSkills().size(); i++) {
            if (i > 0) sb.append(";");
            Skill skill = user.getSkills().get(i);
            sb.append(skill.getName()).append("(").append(skill.getLevel()).append(")");
        }
        sb.append("|");

        sb.append(user.getAvailabilityHoursPerWeek()).append("|");
        sb.append(user.getRole()).append("|");
        sb.append(user.getGoal()).append("|");
        sb.append(user.getMode());
        if (user.getTeamSize() != null) {
            sb.append("|").append(user.getTeamSize());
        }

        return sb.toString();
    }

    private User stringToUser(String data) {
        try {
            String[] parts = data.split("\\|");
            if (parts.length < 7) return null;

            String name = parts[0];
            String email = parts[1];

            // Parse skills
            List<Skill> skills = new ArrayList<>();
            if (!parts[2].isEmpty()) {
                String[] skillStrings = parts[2].split(";");
                for (String skillStr : skillStrings) {
                    skills.add(skillManager.createSkill(
                        skillStr.substring(0, skillStr.indexOf("(")),
                        Skill.SkillLevel.valueOf(skillStr.substring(skillStr.indexOf("(") + 1, skillStr.indexOf(")")))
                    ));
                }
            }

            int availability = Integer.parseInt(parts[3]);
            User.Role role = User.Role.valueOf(parts[4]);
            User.Goal goal = User.Goal.valueOf(parts[5]);
            User.Mode mode = User.Mode.valueOf(parts[6]);
            Integer teamSize = (parts.length > 7) ? Integer.parseInt(parts[7]) : null;

            return new User(name, email, skills, availability, role, goal, mode, teamSize);
        } catch (Exception e) {
            System.err.println("Error parsing user data: " + data);
            return null;
        }
    }
}