import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a user in the SkillSetter system.
 */
public class User {
    private final String name;
    private final String email;
    private final List<String> skills;
    private final SkillLevel skillLevel;
    private final int availabilityHoursPerWeek;
    private final Role role;
    private final Goal goal;
    private final Mode mode;

    public User(String name, String email, List<String> skills, SkillLevel skillLevel,
                int availabilityHoursPerWeek, Role role, Goal goal, Mode mode) {
        this.name = name.trim();
        this.email = email.trim().toLowerCase();
        this.skills = new ArrayList<>();
        for (String skill : skills) {
            if (skill != null && !skill.trim().isEmpty()) {
                this.skills.add(skill.trim());
            }
        }
        this.skillLevel = skillLevel;
        this.availabilityHoursPerWeek = availabilityHoursPerWeek;
        this.role = role;
        this.goal = goal;
        this.mode = mode;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public List<String> getSkills() {
        return Collections.unmodifiableList(skills);
    }

    public SkillLevel getSkillLevel() {
        return skillLevel;
    }

    public int getAvailabilityHoursPerWeek() {
        return availabilityHoursPerWeek;
    }

    public Role getRole() {
        return role;
    }

    public Goal getGoal() {
        return goal;
    }

    public Mode getMode() {
        return mode;
    }

    public boolean hasSkill(String skill) {
        for (String userSkill : skills) {
            if (userSkill.equalsIgnoreCase(skill)) {
                return true;
            }
        }
        return false;
    }

    public String getSummary() {
        return String.format("%s (%s) | Skills: %s | Level: %s | Availability: %d hrs/week | Role: %s | Goal: %s | Mode: %s",
                name, email, skills, skillLevel, availabilityHoursPerWeek, role, goal, mode);
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof User)) {
            return false;
        }
        User user = (User) other;
        return email.equalsIgnoreCase(user.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(email.toLowerCase());
    }

    public enum SkillLevel {
        BEGINNER,
        INTERMEDIATE,
        ADVANCED
    }

    public enum Role {
        LEADER,
        TEAMMATE,
        LEARNER
    }

    public enum Goal {
        HACKATHON,
        PBL,
        STARTUP,
        STUDY
    }

    public enum Mode {
        JOIN,
        BUILD
    }
}
