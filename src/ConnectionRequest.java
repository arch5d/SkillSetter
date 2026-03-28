public class ConnectionRequest {
    private final int id;
    private final String senderEmail;
    private final String senderName;
    private final String receiverEmail;
    private final String receiverName;
    private final String status;

    public ConnectionRequest(int id, String senderEmail, String senderName, String receiverEmail, String receiverName, String status) {
        this.id = id;
        this.senderEmail = senderEmail;
        this.senderName = senderName;
        this.receiverEmail = receiverEmail;
        this.receiverName = receiverName;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public String getSenderEmail() {
        return senderEmail;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getReceiverEmail() {
        return receiverEmail;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public String getStatus() {
        return status;
    }
}