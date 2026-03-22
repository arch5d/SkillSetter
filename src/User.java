import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

//Represents a user in the SkillSetter system.

public class User {
    private final String name;
    private final String email;
    private final List<Skill> skills;
    private final int availabilityHoursPerWeek;
    private final Role role;
    private final Goal goal;
    private final Mode mode;
    private final Integer teamSize; // Only for leaders

    public User(String name, String email, List<Skill> skills,
                int availabilityHoursPerWeek, Role role, Goal goal, Mode mode, Integer teamSize) {
        this.name = name.trim();
        this.email = email.trim().toLowerCase();
        this.skills = new ArrayList<>(skills);
        this.availabilityHoursPerWeek = availabilityHoursPerWeek;
        this.role = role;
        this.goal = goal;
        this.mode = mode;
        this.teamSize = (role == Role.LEADER) ? teamSize : null;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public List<Skill> getSkills() {
        return Collections.unmodifiableList(skills);
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

    public Integer getTeamSize() {
        return teamSize;
    }

    public boolean hasSkill(String skillName) {
        for (Skill skill : skills) {
            if (skill.getName().equalsIgnoreCase(skillName)) {
                return true;
            }
        }
        return false;
    }

    public Skill getSkill(String skillName) {
        for (Skill skill : skills) {
            if (skill.getName().equalsIgnoreCase(skillName)) {
                return skill;
            }
        }
        return null;
    }

    public String getSummary() {
        String teamSizeStr = (teamSize != null) ? " | Team Size: " + teamSize : "";
        return String.format("%s (%s) | Skills: %s | Availability: %d hrs/week | Role: %s | Goal: %s | Mode: %s%s",
                name, email, skills, availabilityHoursPerWeek, role, goal, mode, teamSizeStr);
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
