/**
 * Represents a connection request between two users.
 */
public class ConnectionRequest {
    private final User sender;
    private final User receiver;
    private Status status;

    public ConnectionRequest(User sender, User receiver) {
        this.sender = sender;
        this.receiver = receiver;
        this.status = Status.PENDING;
    }

    public User getSender() {
        return sender;
    }

    public User getReceiver() {
        return receiver;
    }

    public Status getStatus() {
        return status;
    }

    public void accept() {
        status = Status.ACCEPTED;
    }

    public void reject() {
        status = Status.REJECTED;
    }

    public boolean isPendingFor(User user) {
        return status == Status.PENDING && receiver.equals(user);
    }

    public String getContactDetailsFor(User viewer) {
        if (status == Status.ACCEPTED) {
            if (viewer.equals(sender)) {
                return receiver.getEmail();
            }
            if (viewer.equals(receiver)) {
                return sender.getEmail();
            }
        }
        return "Hidden until accepted";
    }

    public String getSummaryFor(User viewer) {
        User otherUser = viewer.equals(sender) ? receiver : sender;
        return String.format("%s -> %s | Status: %s | Contact: %s",
                sender.getName(), receiver.getName(), status, getContactDetailsFor(viewer));
    }

    public enum Status {
        PENDING,
        ACCEPTED,
        REJECTED
    }
}
