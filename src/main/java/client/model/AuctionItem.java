package client.model;

public class AuctionItem {
    private String highestBidder = null;   // username of current highest bidder
    private String highestBidderPhone = null;
    private String id;
    private String name;
    private String description;
    private String category;
    private String imagePath;
    private String sellerUsername;

    private double startPrice;
    private double currentBid;
    private double stepPrice;        // minimum raise per bid

    private String endTime;          // ISO format: "2025-06-01T23:59:00"
    private boolean closed = false;

    public AuctionItem() {}

    public AuctionItem(String id, String name, String description,
                       String category, String imagePath,
                       String sellerUsername, double startPrice,
                       double stepPrice, String endTime) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.imagePath = imagePath;
        this.sellerUsername = sellerUsername;
        this.startPrice = startPrice;
        this.currentBid = startPrice;
        this.stepPrice = stepPrice;
        this.endTime = endTime;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public String getImagePath() { return imagePath; }
    public String getSellerUsername() { return sellerUsername; }
    public double getStartPrice() { return startPrice; }
    public double getCurrentBid() { return currentBid; }
    public double getStepPrice() { return stepPrice; }
    public String getEndTime() { return endTime; }
    public boolean isClosed() { return closed; }
    public void setCurrentBid(double currentBid) { this.currentBid = currentBid; }
    public void setClosed(boolean closed) { this.closed = closed; }
    public String getHighestBidder() { return highestBidder; }
    public String getHighestBidderPhone() { return highestBidderPhone; }
    public void setHighestBidder(String highestBidder) { this.highestBidder = highestBidder; }
    public void setHighestBidderPhone(String highestBidderPhone) { this.highestBidderPhone = highestBidderPhone; }
}
