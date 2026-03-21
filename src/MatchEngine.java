import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Calculates compatibility scores between users.
 */
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

        int skillScore = calculateSkillScore(firstUser, secondUser, reasons);
        score += skillScore;

        if (firstUser.getGoal() == secondUser.getGoal()) {
            score += 30;
            reasons.add("Same goal: " + firstUser.getGoal());
        } else {
            reasons.add("Different goals can bring diverse perspectives");
        }

        int availabilityDifference = Math.abs(firstUser.getAvailabilityHoursPerWeek()
                - secondUser.getAvailabilityHoursPerWeek());
        if (availabilityDifference <= 3) {
            score += 25;
            reasons.add("Very similar availability");
        } else if (availabilityDifference <= 6) {
            score += 15;
            reasons.add("Availability is reasonably close");
        } else {
            reasons.add("Availability gap may require planning");
        }

        if (firstUser.getMode() != secondUser.getMode()) {
            score += 10;
            reasons.add("Join/Build modes complement each other");
        }

        if (firstUser.getRole() != secondUser.getRole()) {
            score += 5;
            reasons.add("Different roles can balance the team");
        }

        score = Math.min(score, 100);
        return new MatchResult(secondUser, score, reasons);
    }

    private int calculateSkillScore(User firstUser, User secondUser, List<String> reasons) {
        Set<String> firstSkills = normalizeSkills(firstUser.getSkills());
        Set<String> secondSkills = normalizeSkills(secondUser.getSkills());

        Set<String> sharedSkills = new HashSet<>(firstSkills);
        sharedSkills.retainAll(secondSkills);

        Set<String> uniqueSkills = new HashSet<>(secondSkills);
        uniqueSkills.removeAll(firstSkills);

        int score = 0;
        if (!uniqueSkills.isEmpty()) {
            score += 20;
            reasons.add("Brings complementary skills: " + uniqueSkills);
        }

        if (sharedSkills.isEmpty()) {
            score += 10;
            reasons.add("Low skill overlap helps avoid duplication");
        } else if (sharedSkills.size() <= 2) {
            score += 5;
            reasons.add("Some shared skills can improve collaboration: " + sharedSkills);
        } else {
            reasons.add("High skill overlap may create duplicate strengths");
        }

        return score;
    }

    private Set<String> normalizeSkills(List<String> skills) {
        Set<String> normalized = new HashSet<>();
        for (String skill : skills) {
            normalized.add(skill.toLowerCase());
        }
        return normalized;
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
