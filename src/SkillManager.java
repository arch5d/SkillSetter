import java.util.*;

//Manages the available skills and their levels.
public class SkillManager {
    private static final List<String> PREDEFINED_SKILLS = Arrays.asList(
        "Java", "Python", "JavaScript", "C++", "C#", "Go", "Rust", "PHP",
        "React", "Angular", "Vue.js", "Node.js", "Spring", "Django", "Flask",
        "HTML", "CSS", "UI/UX Design", "Graphic Design", "Mobile Development",
        "Android", "iOS", "Database", "SQL", "MongoDB", "PostgreSQL",
        "Machine Learning", "Data Science", "AI", "DevOps", "Docker", "Kubernetes",
        "AWS", "Azure", "GCP", "Testing", "QA", "Project Management",
        "Agile", "Scrum", "Marketing", "Business Analysis", "Presentation",
        "Public Speaking", "Research", "Documentation", "Git", "Version Control"
    );

    private final Set<String> availableSkills;

    public SkillManager() {
        this.availableSkills = new HashSet<>(PREDEFINED_SKILLS);
    }

    public List<String> getAvailableSkills() {
        return new ArrayList<>(availableSkills);
    }

    public boolean isPredefinedSkill(String skillName) {
        return PREDEFINED_SKILLS.contains(skillName);
    }

    public void addCustomSkill(String skillName) {
        availableSkills.add(skillName.trim());
    }

    public Skill createSkill(String name, Skill.SkillLevel level) {
        String trimmedName = name.trim();
        if (!availableSkills.contains(trimmedName)) {
            addCustomSkill(trimmedName);
        }
        return new Skill(trimmedName, level);
    }

    public List<Skill> parseSkillsInput(String input) {
        List<Skill> skills = new ArrayList<>();
        String[] parts = input.split(",");
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                // Default to INTERMEDIATE if no level specified
                String[] skillParts = trimmed.split("\\(");
                String skillName = skillParts[0].trim();
                Skill.SkillLevel level = Skill.SkillLevel.INTERMEDIATE;

                if (skillParts.length > 1 && skillParts[1].contains(")")) {
                    String levelStr = skillParts[1].replace(")", "").trim().toUpperCase();
                    try {
                        level = Skill.SkillLevel.valueOf(levelStr);
                    } catch (IllegalArgumentException e) {
                        // Keep default level
                    }
                }

                skills.add(createSkill(skillName, level));
            }
        }
        return skills;
    }
}