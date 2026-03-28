import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;

public class DatabaseManager {
    private final String dbUrl;
    private final SkillManager skillManager;

    public DatabaseManager(SkillManager skillManager) {
        this.skillManager = skillManager;
        this.dbUrl = resolveDbUrl();
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("SQLite JDBC driver not found on classpath", e);
        }
        System.out.println("SkillSetter using DB file: " + this.dbUrl.replace("jdbc:sqlite:", ""));
        initTables();
    }

    private String resolveDbUrl() {
        String configuredPath = System.getProperty("skillsetter.db.path");
        if (configuredPath == null || configuredPath.trim().isEmpty()) {
            configuredPath = System.getenv("SKILLSETTER_DB_PATH");
        }

        Path dbFile;
        if (configuredPath != null && !configuredPath.trim().isEmpty()) {
            dbFile = Paths.get(configuredPath.trim());
        } else {
            // Default outside workspace to avoid frontend auto-refresh when DB is updated.
            dbFile = Paths.get(System.getProperty("user.home"), ".skillsetter", "skillsetter.db");
        }

        Path absolute = dbFile.toAbsolutePath();
        Path parent = absolute.getParent();
        if (parent != null) {
            try {
                Files.createDirectories(parent);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to prepare database directory: " + parent, e);
            }
        }

        return "jdbc:sqlite:" + absolute;
    }

    private void initTables() {
        try (Connection conn = openConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                         "email TEXT PRIMARY KEY, " +
                         "name TEXT, " +
                         "availability INTEGER, " +
                         "role TEXT, " +
                         "goal TEXT, " +
                         "mode TEXT, " +
                         "team_size INTEGER)");

            stmt.execute("CREATE TABLE IF NOT EXISTS user_skills (" +
                         "email TEXT, " +
                         "skill_name TEXT, " +
                         "skill_level TEXT, " +
                         "FOREIGN KEY(email) REFERENCES users(email) ON DELETE CASCADE)");

            stmt.execute("CREATE TABLE IF NOT EXISTS requests (" +
                         "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                         "sender_email TEXT, " +
                         "receiver_email TEXT, " +
                         "status TEXT, " +
                         "FOREIGN KEY(sender_email) REFERENCES users(email) ON DELETE CASCADE, " +
                         "FOREIGN KEY(receiver_email) REFERENCES users(email) ON DELETE CASCADE)");

        } catch (SQLException e) {
            throw new IllegalStateException("Failed to initialize database schema", e);
        }
    }

    private Connection openConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(dbUrl);
        try (Statement pragma = conn.createStatement()) {
            pragma.execute("PRAGMA foreign_keys = ON");
        }
        return conn;
    }

    public void saveUser(User user) {
        String upsertUser = "INSERT OR REPLACE INTO users(email, name, availability, role, goal, mode, team_size) VALUES(?,?,?,?,?,?,?)";
        String deleteSkills = "DELETE FROM user_skills WHERE email = ?";
        String insertSkill = "INSERT INTO user_skills(email, skill_name, skill_level) VALUES(?,?,?)";

        try (Connection conn = openConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement pstmt = conn.prepareStatement(upsertUser)) {
                pstmt.setString(1, user.getEmail());
                pstmt.setString(2, user.getName());
                pstmt.setInt(3, user.getAvailabilityHoursPerWeek());
                pstmt.setString(4, user.getRole().name());
                pstmt.setString(5, user.getGoal().name());
                pstmt.setString(6, user.getMode().name());
                if (user.getTeamSize() != null) {
                    pstmt.setInt(7, user.getTeamSize());
                } else {
                    pstmt.setNull(7, Types.INTEGER);
                }
                pstmt.executeUpdate();
            }

            try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSkills)) {
                deleteStmt.setString(1, user.getEmail());
                deleteStmt.executeUpdate();
            }

            try (PreparedStatement pstmtSkills = conn.prepareStatement(insertSkill)) {
                for (Skill skill : user.getSkills()) {
                    pstmtSkills.setString(1, user.getEmail());
                    pstmtSkills.setString(2, skill.getName());
                    pstmtSkills.setString(3, skill.getLevel().name());
                    pstmtSkills.executeUpdate();
                }
            }

            conn.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<User> loadUsers() {
        List<User> users = new ArrayList<>();
        String queryUsers = "SELECT * FROM users";
        String querySkills = "SELECT skill_name, skill_level FROM user_skills WHERE email = ?";

        try (Connection conn = openConnection();
             Statement stmt = conn.createStatement();
             ResultSet rsUsers = stmt.executeQuery(queryUsers)) {

            PreparedStatement pstmtSkills = conn.prepareStatement(querySkills);

            while (rsUsers.next()) {
                String email = rsUsers.getString("email");
                String name = rsUsers.getString("name");
                int availability = rsUsers.getInt("availability");
                User.Role role = User.Role.valueOf(rsUsers.getString("role"));
                User.Goal goal = User.Goal.valueOf(rsUsers.getString("goal"));
                User.Mode mode = User.Mode.valueOf(rsUsers.getString("mode"));
                
                int teamSizeVal = rsUsers.getInt("team_size");
                Integer teamSize = rsUsers.wasNull() ? null : teamSizeVal;

                List<Skill> skills = new ArrayList<>();
                pstmtSkills.setString(1, email);
                ResultSet rsSkills = pstmtSkills.executeQuery();
                while (rsSkills.next()) {
                    skills.add(skillManager.createSkill(
                        rsSkills.getString("skill_name"),
                        Skill.SkillLevel.valueOf(rsSkills.getString("skill_level"))
                    ));
                }
                rsSkills.close();

                users.add(new User(name, email, skills, availability, role, goal, mode, teamSize));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    public User getUserByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }

        String queryUser = "SELECT * FROM users WHERE email = ?";
        String querySkills = "SELECT skill_name, skill_level FROM user_skills WHERE email = ?";

        try (Connection conn = openConnection();
             PreparedStatement userStmt = conn.prepareStatement(queryUser);
             PreparedStatement skillStmt = conn.prepareStatement(querySkills)) {
            userStmt.setString(1, email.trim().toLowerCase());
            try (ResultSet rsUser = userStmt.executeQuery()) {
                if (!rsUser.next()) {
                    return null;
                }

                String normalizedEmail = rsUser.getString("email");
                String name = rsUser.getString("name");
                int availability = rsUser.getInt("availability");
                User.Role role = User.Role.valueOf(rsUser.getString("role"));
                User.Goal goal = User.Goal.valueOf(rsUser.getString("goal"));
                User.Mode mode = User.Mode.valueOf(rsUser.getString("mode"));
                int teamSizeValue = rsUser.getInt("team_size");
                Integer teamSize = rsUser.wasNull() ? null : teamSizeValue;

                List<Skill> skills = new ArrayList<>();
                skillStmt.setString(1, normalizedEmail);
                try (ResultSet rsSkills = skillStmt.executeQuery()) {
                    while (rsSkills.next()) {
                        skills.add(skillManager.createSkill(
                                rsSkills.getString("skill_name"),
                                Skill.SkillLevel.valueOf(rsSkills.getString("skill_level"))));
                    }
                }

                return new User(name, normalizedEmail, skills, availability, role, goal, mode, teamSize);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void deleteUser(String email) {
        String sql = "DELETE FROM users WHERE email = ?";
        try (Connection conn = openConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Requests Methods
    public boolean sendRequest(String senderEmail, String receiverEmail) {
        if (senderEmail == null || receiverEmail == null) {
            return false;
        }
        String normalizedSender = senderEmail.trim().toLowerCase();
        String normalizedReceiver = receiverEmail.trim().toLowerCase();
        if (normalizedSender.equals(normalizedReceiver)) {
            return false;
        }

        if (getUserByEmail(normalizedSender) == null || getUserByEmail(normalizedReceiver) == null) {
            return false;
        }

        String existingSql = "SELECT id FROM requests WHERE sender_email = ? AND receiver_email = ? AND status = 'PENDING'";
        String inverseExistingSql = "SELECT id FROM requests WHERE sender_email = ? AND receiver_email = ? AND status = 'PENDING'";
        String sql = "INSERT INTO requests(sender_email, receiver_email, status) VALUES(?, ?, 'PENDING')";

        try (Connection conn = openConnection();
             PreparedStatement checkStmt = conn.prepareStatement(existingSql);
             PreparedStatement inverseStmt = conn.prepareStatement(inverseExistingSql);
             PreparedStatement insertStmt = conn.prepareStatement(sql)) {
            checkStmt.setString(1, normalizedSender);
            checkStmt.setString(2, normalizedReceiver);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next()) {
                    return false;
                }
            }

            inverseStmt.setString(1, normalizedReceiver);
            inverseStmt.setString(2, normalizedSender);
            try (ResultSet rsInverse = inverseStmt.executeQuery()) {
                if (rsInverse.next()) {
                    return false;
                }
            }

            insertStmt.setString(1, normalizedSender);
            insertStmt.setString(2, normalizedReceiver);
            insertStmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateRequestStatus(int requestId, String receiverEmail, String status) {
        if (receiverEmail == null || status == null) {
            return false;
        }
        String normalizedStatus = status.trim().toUpperCase();
        if (!"ACCEPTED".equals(normalizedStatus) && !"REJECTED".equals(normalizedStatus)) {
            return false;
        }

        String sql = "UPDATE requests SET status = ? WHERE id = ? AND receiver_email = ? AND status = 'PENDING'";
        try (Connection conn = openConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, normalizedStatus);
            pstmt.setInt(2, requestId);
            pstmt.setString(3, receiverEmail.trim().toLowerCase());
            return pstmt.executeUpdate() == 1;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<ConnectionRequest> getIncomingRequests(String receiverEmail) {
        List<ConnectionRequest> requests = new ArrayList<>();
        String sql = "SELECT r.id, r.sender_email, u.name AS sender_name, r.status " +
                "FROM requests r JOIN users u ON u.email = r.sender_email WHERE r.receiver_email = ? ORDER BY r.id DESC";
        try (Connection conn = openConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, receiverEmail);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                requests.add(new ConnectionRequest(
                    rs.getInt("id"),
                    rs.getString("sender_email"),
                    rs.getString("sender_name"),
                    receiverEmail,
                    null,
                    rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return requests;
    }

    public List<ConnectionRequest> getSentRequests(String senderEmail) {
        List<ConnectionRequest> requests = new ArrayList<>();
        String sql = "SELECT r.id, r.receiver_email, u.name AS receiver_name, r.status " +
                "FROM requests r JOIN users u ON u.email = r.receiver_email WHERE r.sender_email = ? ORDER BY r.id DESC";
        try (Connection conn = openConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, senderEmail);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                requests.add(new ConnectionRequest(
                    rs.getInt("id"),
                    senderEmail,
                    null,
                    rs.getString("receiver_email"),
                    rs.getString("receiver_name"),
                    rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return requests;
    }

    public boolean hasAcceptedConnection(String userEmail, String otherEmail) {
        String sql = "SELECT id FROM requests " +
                "WHERE status = 'ACCEPTED' AND ((sender_email = ? AND receiver_email = ?) OR (sender_email = ? AND receiver_email = ?)) LIMIT 1";
        try (Connection conn = openConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            String first = userEmail.trim().toLowerCase();
            String second = otherEmail.trim().toLowerCase();
            pstmt.setString(1, first);
            pstmt.setString(2, second);
            pstmt.setString(3, second);
            pstmt.setString(4, first);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
