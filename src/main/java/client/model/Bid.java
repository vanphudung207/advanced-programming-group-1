package client.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Bid {
    private String itemId;
    private String bidderUsername;
    private double amount;
    private String timestamp;

    public Bid() {}

    public Bid(String itemId, String bidderUsername, double amount) {
        this.itemId = itemId;
        this.bidderUsername = bidderUsername;
        this.amount = amount;
        // Auto-stamp the time when bid is created
        this.timestamp = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public String getItemId() { return itemId; }
    public String getBidderUsername() { return bidderUsername; }
    public double getAmount() { return amount; }
    public String getTimestamp() { return timestamp; }
}