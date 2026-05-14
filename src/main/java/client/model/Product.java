package client.model;

public class Product {

    private String id;
    private String firebaseKey;
    private String name;
    private String description;
    private double currentBid;
    private String timeRemaining;
    private String imagePath;
    private String sellerUsername;
    private String category;
    private double stepPrice;
    private String status = "active";
    private long endTime = 0L;
    private String highestBidder = null;
    private String highestBidderPhone = null;
    private String highestBidderEmail = null;

    public Product(String id, String name, String description,
                   double currentBid, String timeRemaining,
                   String imagePath, String sellerUsername, String category,
                   double stepPrice, String status, long endTime) {
        this.id = id;
        this.firebaseKey = id;
        this.name = name;
        this.description = description;
        this.currentBid = currentBid;
        this.timeRemaining = timeRemaining;
        this.imagePath = imagePath;
        this.sellerUsername = sellerUsername;
        this.category = category;
        this.stepPrice = stepPrice;
        this.status = status != null ? status : "active";
        this.endTime = endTime;
    }

    public Product(String id, String name, double currentBid, String timeRemaining,
                   String imagePath, String sellerUsername, String category,
                   double stepPrice, String status, long endTime) {
        this(id, name, null, currentBid, timeRemaining, imagePath, sellerUsername,
             category, stepPrice, status, endTime);
    }

    public Product(String id, String name, double currentBid, String timeRemaining,
                   String imagePath, String description, String sellerUsername,
                   String category, double stepPrice, String status, long endTime) {
        this(id, name, description, currentBid, timeRemaining, imagePath,
             sellerUsername, category, stepPrice, status, endTime);
    }

    public Product(String id, String name, double currentBid, String timeRemaining,
                   String imagePath, String sellerUsername, String category,
                   double stepPrice, String status, int secondsRemaining) {
        this(id, name, null, currentBid, timeRemaining, imagePath, sellerUsername,
             category, stepPrice, status,
             secondsRemaining > 0
                 ? System.currentTimeMillis() + (long) secondsRemaining * 1000
                 : 0L);
    }

    public Product(String id, String name, double currentBid, String timeRemaining,
                   String imagePath, String sellerUsername, String category) {
        this(id, name, null, currentBid, timeRemaining, imagePath, sellerUsername,
             category, 0.0, "active", 60);
    }

    public Product(String id, String name, double currentBid, String timeRemaining,
                   String imagePath, String sellerUsername) {
        this(id, name, null, currentBid, timeRemaining, imagePath, sellerUsername,
             "Khác", 0.0, "active", 60);
    }

    public int getSecondsRemainingNow() {
        if ("ended".equals(status) || endTime <= 0) {
            return 0;
        }
        long diff = endTime - System.currentTimeMillis();
        return diff > 0 ? (int) (diff / 1000) : 0;
    }

    public boolean isEnded() {
        return "ended".equals(status) || (endTime > 0 && System.currentTimeMillis() >= endTime);
    }

    public String getId() { return id; }
    public String getFirebaseKey() { return firebaseKey; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public double getCurrentBid() { return currentBid; }
    public String getTimeRemaining() { return timeRemaining; }
    public String getImagePath() { return imagePath; }
    public String getSellerUsername() { return sellerUsername; }
    public String getCategory() { return category; }
    public double getStepPrice() { return stepPrice; }
    public String getStatus() { return status; }
    public long getEndTime() { return endTime; }
    public String getHighestBidder() { return highestBidder; }
    public String getHighestBidderPhone() { return highestBidderPhone; }
    public String getHighestBidderEmail() { return highestBidderEmail; }

    public void setId(String value) { this.id = value; }
    public void setFirebaseKey(String value) { this.firebaseKey = value; }
    public void setName(String value) { this.name = value; }
    public void setDescription(String value) { this.description = value; }
    public void setCurrentBid(double value) { this.currentBid = value; }
    public void setTimeRemaining(String value) { this.timeRemaining = value; }
    public void setImagePath(String value) { this.imagePath = value; }
    public void setSellerUsername(String value) { this.sellerUsername = value; }
    public void setCategory(String value) { this.category = value; }
    public void setStepPrice(double value) { this.stepPrice = value; }
    public void setStatus(String value) { this.status = value; }
    public void setEndTime(long value) { this.endTime = value; }
    public void setHighestBidder(String value) { this.highestBidder = value; }
    public void setHighestBidderPhone(String value) { this.highestBidderPhone = value; }
    public void setHighestBidderEmail(String value) { this.highestBidderEmail = value; }
}
