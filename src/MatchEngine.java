import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

//Calculates compatibility scores between users.
public class MatchEngine {
    public List<MatchResult> findMatches(User currentUser, List<User> allUsers) {
        List<MatchResult> results = new ArrayList<>();

        for (User candidate : allUsers) {
            if (!candidate.equals(currentUser)) {
                results.add(calculateMatch(currentUser, candidate));
            }
        }

        results.sort(Comparator.comparingInt(MatchResult::getScore).reversed());
        return results;
    }

    public MatchResult calculateMatch(User firstUser, User secondUser) {
        int score = 0;
        List<String> reasons = new ArrayList<>();

        // Complementary skills with level consideration
        int skillScore = calculateSkillCompatibility(firstUser, secondUser, reasons);
        score += skillScore;

        // Similar time devoted
        int timeScore = calculateTimeCompatibility(firstUser, secondUser, reasons);
        score += timeScore;

        // Leader/Join team matching
        int roleScore = calculateRoleCompatibility(firstUser, secondUser, reasons);
        score += roleScore;

        // Same goal bonus
        if (firstUser.getGoal() == secondUser.getGoal()) {
            score += 15;
            reasons.add("Same goal: " + firstUser.getGoal());
        } else {
            reasons.add("Different goals can bring diverse perspectives");
        }

        score = Math.min(score, 100);
        return new MatchResult(secondUser, score, reasons);
    }

    private int calculateSkillCompatibility(User firstUser, User secondUser, List<String> reasons) {
        Set<String> firstSkillNames = new HashSet<>();
        Map<String, Skill.SkillLevel> firstSkillLevels = new HashMap<>();
        for (Skill skill : firstUser.getSkills()) {
            firstSkillNames.add(skill.getName().toLowerCase());
            firstSkillLevels.put(skill.getName().toLowerCase(), skill.getLevel());
        }

        Set<String> secondSkillNames = new HashSet<>();
        Map<String, Skill.SkillLevel> secondSkillLevels = new HashMap<>();
        for (Skill skill : secondUser.getSkills()) {
            secondSkillNames.add(skill.getName().toLowerCase());
            secondSkillLevels.put(skill.getName().toLowerCase(), skill.getLevel());
        }

        // Find complementary skills (skills one has that the other doesn't)
        Set<String> complementarySkills = new HashSet<>();
        complementarySkills.addAll(firstSkillNames);
        complementarySkills.removeAll(secondSkillNames);

        Set<String> complementarySkillsOther = new HashSet<>();
        complementarySkillsOther.addAll(secondSkillNames);
        complementarySkillsOther.removeAll(firstSkillNames);

        // Find shared skills and check level compatibility
        Set<String> sharedSkills = new HashSet<>(firstSkillNames);
        sharedSkills.retainAll(secondSkillNames);

        int score = 0;

        // Complementary skills bonus
        if (!complementarySkills.isEmpty() || !complementarySkillsOther.isEmpty()) {
            score += 25;
            Set<String> allComplementary = new HashSet<>();
            allComplementary.addAll(complementarySkills);
            allComplementary.addAll(complementarySkillsOther);
            reasons.add("Complementary skills: " + allComplementary);
        }

        // Level compatibility for shared skills
        int levelBonus = 0;
        for (String sharedSkill : sharedSkills) {
            Skill.SkillLevel level1 = firstSkillLevels.get(sharedSkill);
            Skill.SkillLevel level2 = secondSkillLevels.get(sharedSkill);

            if (level1 == level2) {
                levelBonus += 5; // Same level - good for collaboration
            } else if (Math.abs(level1.ordinal() - level2.ordinal()) == 1) {
                levelBonus += 3; // Adjacent levels - good for mentoring
            }
        }
        if (levelBonus > 0) {
            score += Math.min(levelBonus, 15);
            reasons.add("Skill level compatibility bonus");
        }

        // Shared skills bonus
        if (!sharedSkills.isEmpty()) {
            score += Math.min(sharedSkills.size() * 3, 15);
            reasons.add("Shared skills for collaboration: " + sharedSkills);
        }

        return score;
    }

    private int calculateTimeCompatibility(User firstUser, User secondUser, List<String> reasons) {
        int diff = Math.abs(firstUser.getAvailabilityHoursPerWeek() - secondUser.getAvailabilityHoursPerWeek());
        if (diff <= 2) {
            reasons.add("Very similar time commitment");
            return 20;
        } else if (diff <= 5) {
            reasons.add("Reasonable time commitment match");
            return 10;
        } else {
            reasons.add("Different time commitments may need coordination");
            return 0;
        }
    }

    private int calculateRoleCompatibility(User firstUser, User secondUser, List<String> reasons) {
        User.Role role1 = firstUser.getRole();
        User.Role role2 = secondUser.getRole();
        User.Mode mode1 = firstUser.getMode();
        User.Mode mode2 = secondUser.getMode();

        int score = 0;

        // Leader + Join team compatibility
        if ((role1 == User.Role.LEADER && mode2 == User.Mode.JOIN) ||
            (role2 == User.Role.LEADER && mode1 == User.Mode.JOIN)) {
            score += 25;
            reasons.add("Leader-Join team match");

            // Check team size compatibility
            User leader = (role1 == User.Role.LEADER) ? firstUser : secondUser;
            if (leader.getTeamSize() != null && leader.getTeamSize() > 1) {
                score += 5;
                reasons.add("Team size allows for additional members");
            }
        }

        // Different roles for balance
        if (role1 != role2) {
            score += 10;
            reasons.add("Different roles provide team balance");
        }

        // Build/Join mode complementarity
        if (mode1 != mode2) {
            score += 5;
            reasons.add("Build/Join modes complement each other");
        }

        return score;
    }

    public static class MatchResult {
        private final User matchedUser;
        private final int score;
        private final List<String> reasons;

        public MatchResult(User matchedUser, int score, List<String> reasons) {
            this.matchedUser = matchedUser;
            this.score = score;
            this.reasons = new ArrayList<>(reasons);
        }

        public User getMatchedUser() {
            return matchedUser;
        }

        public int getScore() {
            return score;
        }

        public List<String> getReasons() {
            return Collections.unmodifiableList(reasons);
        }

        public String toDisplayString() {
            return String.format("%s - %d%% match | Reasons: %s",
                    matchedUser.getName(), score, reasons);
        }
    }
}
