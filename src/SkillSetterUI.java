import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

/**
 * Minimal Swing user interface for SkillSetter.
 */
public class SkillSetterUI {
    private final SkillSetterApp app;
    private final JFrame frame;
    private final JTextArea outputArea;
    private final DefaultListModel<ConnectionRequest> requestListModel;
    private final JList<ConnectionRequest> requestList;

    public SkillSetterUI(SkillSetterApp app) {
        this.app = app;
        this.frame = new JFrame("SkillSetter");
        this.outputArea = new JTextArea(18, 45);
        this.requestListModel = new DefaultListModel<>();
        this.requestList = new JList<>(requestListModel);
        buildUi();
    }

    private void buildUi() {
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout(10, 10));

        JPanel buttonPanel = new JPanel(new GridLayout(1, 3, 10, 10));
        JButton registerButton = new JButton("Register");
        JButton matchesButton = new JButton("View Matches");
        JButton requestsButton = new JButton("Requests");

        registerButton.addActionListener(event -> showRegisterForm());
        matchesButton.addActionListener(event -> showMatches());
        requestsButton.addActionListener(event -> showRequestsPanel());

        buttonPanel.add(registerButton);
        buttonPanel.add(matchesButton);
        buttonPanel.add(requestsButton);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));

        outputArea.setEditable(false);
        outputArea.setLineWrap(true);
        outputArea.setWrapStyleWord(true);

        requestList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        frame.add(buttonPanel, BorderLayout.NORTH);
        frame.add(new JScrollPane(outputArea), BorderLayout.CENTER);
        frame.add(new JScrollPane(requestList), BorderLayout.EAST);
        frame.pack();
        frame.setLocationRelativeTo(null);
        refreshRequests();
        showUsersOverview();
    }

    public void show() {
        frame.setVisible(true);
    }

    private void showRegisterForm() {
        JTextField nameField = new JTextField();
        JTextField emailField = new JTextField();
        JTextField skillsField = new JTextField();
        JTextField availabilityField = new JTextField();
        JComboBox<User.SkillLevel> levelBox = new JComboBox<>(User.SkillLevel.values());
        JComboBox<User.Role> roleBox = new JComboBox<>(User.Role.values());
        JComboBox<User.Goal> goalBox = new JComboBox<>(User.Goal.values());
        JComboBox<User.Mode> modeBox = new JComboBox<>(User.Mode.values());

        JPanel panel = new JPanel(new GridLayout(0, 2, 6, 6));
        panel.add(new JLabel("Name"));
        panel.add(nameField);
        panel.add(new JLabel("Email"));
        panel.add(emailField);
        panel.add(new JLabel("Skills (comma separated)"));
        panel.add(skillsField);
        panel.add(new JLabel("Availability (hours/week)"));
        panel.add(availabilityField);
        panel.add(new JLabel("Skill Level"));
        panel.add(levelBox);
        panel.add(new JLabel("Role"));
        panel.add(roleBox);
        panel.add(new JLabel("Goal"));
        panel.add(goalBox);
        panel.add(new JLabel("Mode"));
        panel.add(modeBox);

        int result = JOptionPane.showConfirmDialog(frame, panel, "Register User",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            try {
                app.registerUser(
                        nameField.getText(),
                        emailField.getText(),
                        SkillSetterApp.parseSkills(skillsField.getText()),
                        (User.SkillLevel) levelBox.getSelectedItem(),
                        Integer.parseInt(availabilityField.getText().trim()),
                        (User.Role) roleBox.getSelectedItem(),
                        (User.Goal) goalBox.getSelectedItem(),
                        (User.Mode) modeBox.getSelectedItem()
                );
                showUsersOverview();
                JOptionPane.showMessageDialog(frame, "User registered successfully.");
            } catch (NumberFormatException exception) {
                JOptionPane.showMessageDialog(frame, "Availability must be a number.");
            }
        }
    }

    private void showMatches() {
        User selectedUser = promptForUser("Choose a user to view matches");
        if (selectedUser == null) {
            return;
        }

        List<MatchEngine.MatchResult> matches = app.getMatchesForUser(selectedUser);
        StringBuilder builder = new StringBuilder();
        builder.append("Matches for ").append(selectedUser.getName()).append(":\n\n");
        for (MatchEngine.MatchResult match : matches) {
            builder.append(match.toDisplayString()).append("\n");
        }

        if (matches.isEmpty()) {
            builder.append("No matches available yet.");
        }

        outputArea.setText(builder.toString());

        Object[] actions = {"Send Request", "Close"};
        int option = JOptionPane.showOptionDialog(frame,
                "Do you want to send a connection request from " + selectedUser.getName() + "?",
                "Send Request", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, actions, actions[0]);

        if (option == 0) {
            User receiver = promptForUser("Choose the receiver");
            if (receiver != null && !receiver.equals(selectedUser)) {
                app.sendRequest(selectedUser, receiver);
                refreshRequests();
                JOptionPane.showMessageDialog(frame, "Request sent.");
            }
        }
    }

    private void showRequestsPanel() {
        refreshRequests();
        ConnectionRequest selectedRequest = requestList.getSelectedValue();
        if (selectedRequest == null) {
            outputArea.setText("Select a request from the list on the right, then press Requests again.");
            return;
        }

        Object[] actions = {"Accept", "Reject", "Close"};
        int choice = JOptionPane.showOptionDialog(frame,
                "Respond to request from " + selectedRequest.getSender().getName() +
                        " to " + selectedRequest.getReceiver().getName(),
                "Request Response", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,
                null, actions, actions[0]);

        if (choice == 0) {
            app.respondToRequest(selectedRequest, true);
        } else if (choice == 1) {
            app.respondToRequest(selectedRequest, false);
        }

        refreshRequests();
        showUsersOverview();
    }

    private void refreshRequests() {
        requestListModel.clear();
        for (ConnectionRequest request : app.getRequests()) {
            requestListModel.addElement(request);
        }
        requestList.setCellRenderer((list, value, index, isSelected, cellHasFocus) ->
                new JLabel(value.getSender().getName() + " -> " + value.getReceiver().getName()
                        + " (" + value.getStatus() + ")"));
    }

    private void showUsersOverview() {
        StringBuilder builder = new StringBuilder("Registered users:\n\n");
        for (User user : app.getUsers()) {
            builder.append(user.getSummary()).append("\n");
        }
        builder.append("\nAccepted requests reveal contact email to both users.");
        outputArea.setText(builder.toString());
    }

    private User promptForUser(String title) {
        List<User> users = app.getUsers();
        if (users.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "No users available.");
            return null;
        }

        return (User) JOptionPane.showInputDialog(frame, title, "Select User",
                JOptionPane.PLAIN_MESSAGE, null, users.toArray(), users.get(0));
    }
}
