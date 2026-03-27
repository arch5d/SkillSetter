import java.util.List;

public class JSONHelper {
    public static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    public static String toJSON(User user) {
        if (user == null) return "null";
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"name\":\"").append(escape(user.getName())).append("\",");
        sb.append("\"email\":\"").append(escape(user.getEmail())).append("\",");
        
        sb.append("\"skills\":[");
        for (int i = 0; i < user.getSkills().size(); i++) {
            Skill s = user.getSkills().get(i);
            sb.append("{")
              .append("\"name\":\"").append(escape(s.getName())).append("\",")
              .append("\"level\":\"").append(s.getLevel().name()).append("\"")
              .append("}");
            if (i < user.getSkills().size() - 1) sb.append(",");
        }
        sb.append("],");
        
        sb.append("\"availability\":").append(user.getAvailabilityHoursPerWeek()).append(",");
        sb.append("\"role\":\"").append(user.getRole().name()).append("\",");
        sb.append("\"goal\":\"").append(user.getGoal().name()).append("\",");
        sb.append("\"mode\":\"").append(user.getMode().name()).append("\"");
        
        if (user.getTeamSize() != null) {
            sb.append(",\"teamSize\":").append(user.getTeamSize());
        }
        sb.append("}");
        return sb.toString();
    }
    
    public static String toJSON(List<User> users) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < users.size(); i++) {
            sb.append(toJSON(users.get(i)));
            if (i < users.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    public static String matchResultToJSON(MatchEngine.MatchResult result) {
        String reasonsStr = String.join(", ", result.getReasons());
        
        StringBuilder compSkills = new StringBuilder("[");
        List<Skill> cSkills = result.getComplementarySkills();
        for (int i = 0; i < cSkills.size(); i++) {
            compSkills.append("\"").append(escape(cSkills.get(i).getName())).append("\"");
            if (i < cSkills.size() - 1) compSkills.append(",");
        }
        compSkills.append("]");

        return "{" +
               "\"user\":" + toJSON(result.getMatchedUser()) + "," +
               "\"score\":" + result.getScore() + "," +
               "\"reason\":\"" + escape(reasonsStr) + "\"," +
               "\"complementarySkills\":" + compSkills.toString() +
               "}";
    }

    public static String matchResultsToJSON(List<MatchEngine.MatchResult> results) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < results.size(); i++) {
            sb.append(matchResultToJSON(results.get(i)));
            if (i < results.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    public static String connectionRequestsToJSON(List<ConnectionRequest> requests) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < requests.size(); i++) {
            ConnectionRequest r = requests.get(i);
            sb.append("{")
              .append("\"id\":").append(r.getId()).append(",")
              .append("\"senderEmail\":\"").append(escape(r.getSenderEmail())).append("\",")
                            .append("\"senderName\":\"").append(escape(r.getSenderName())).append("\",")
              .append("\"receiverEmail\":\"").append(escape(r.getReceiverEmail())).append("\",")
              .append("\"status\":\"").append(escape(r.getStatus())).append("\"")
              .append("}");
            if (i < requests.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }
}
