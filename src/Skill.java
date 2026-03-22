import java.util.Objects;

//Represents a skill with its level.
public class Skill {
    private final String name;
    private final SkillLevel level;

    public Skill(String name, SkillLevel level) {
        this.name = name.trim();
        this.level = level;
    }

    public String getName() {
        return name;
    }

    public SkillLevel getLevel() {
        return level;
    }

    @Override
    public String toString() {
        return name + " (" + level + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Skill)) return false;
        Skill skill = (Skill) obj;
        return Objects.equals(name.toLowerCase(), skill.name.toLowerCase()) &&
               level == skill.level;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name.toLowerCase(), level);
    }

    public enum SkillLevel {
        BEGINNER,
        INTERMEDIATE,
        ADVANCED,
        EXPERT
    }
}