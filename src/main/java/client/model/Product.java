package client.model;

public class Product {

    private String id;
    private String firebaseKey;
    private String name;
    private double currentBid;
    private String timeRemaining;
    private String imagePath;
    private String sellerUsername;
    private String category;
    private double stepPrice;

    // Trạng thái phiên
    private String status = "active";       // "active" | "ended"

    /**
     * endTime: Unix timestamp (mili-giây) lúc phiên kết thúc.
     * Đây là nguồn sự thật duy nhất cho thời gian còn lại.
     * Không dùng secondsRemaining để tính timer nữa.
     */
    private long endTime = 0L;

    // Thông tin người thắng
    private String highestBidder      = null;
    private String highestBidderPhone = null;
    private String highestBidderEmail = null;

    // =========================================================================
    // CONSTRUCTOR ĐẦY ĐỦ (dùng khi load từ Firebase — có endTime)
    // =========================================================================
    public Product(String id, String name, double currentBid, String timeRemaining,
                   String imagePath, String sellerUsername, String category,
                   double stepPrice, String status, long endTime) {
        this.id            = id;
        this.firebaseKey   = id;
        this.name          = name;
        this.currentBid    = currentBid;
        this.timeRemaining = timeRemaining;
        this.imagePath     = imagePath;
        this.sellerUsername= sellerUsername;
        this.category      = category;
        this.stepPrice     = stepPrice;
        this.status        = (status != null) ? status : "active";
        this.endTime       = endTime;
    }

    // Constructor tương thích ngược — dùng khi tạo sản phẩm mới (endTime chưa biết)
    public Product(String id, String name, double currentBid, String timeRemaining,
                   String imagePath, String sellerUsername, String category,
                   double stepPrice, String status, int secondsRemaining) {
        this(id, name, currentBid, timeRemaining, imagePath, sellerUsername,
             category, stepPrice, status,
             // Chuyển secondsRemaining → endTime tuyệt đối
             secondsRemaining > 0
                 ? System.currentTimeMillis() + (long) secondsRemaining * 1000
                 : 0L);
    }

    public Product(String id, String name, double currentBid, String timeRemaining,
                   String imagePath, String sellerUsername, String category) {
        this(id, name, currentBid, timeRemaining, imagePath, sellerUsername, category,
             0.0, "active", 60);
    }

    public Product(String id, String name, double currentBid, String timeRemaining,
                   String imagePath, String sellerUsername) {
        this(id, name, currentBid, timeRemaining, imagePath, sellerUsername, "Khác",
             0.0, "active", 60);
    }

    // =========================================================================
    // TIỆN ÍCH TÍNH THỜI GIAN CÒN LẠI TỪ endTime — chính xác dù thoát vào lại
    // =========================================================================

    /** Số giây còn lại tính từ thời điểm gọi hàm này (realtime, không drift) */
    public int getSecondsRemainingNow() {
        if ("ended".equals(status) || endTime <= 0) return 0;
        long diff = endTime - System.currentTimeMillis();
        return diff > 0 ? (int) (diff / 1000) : 0;
    }

    /** Phiên đã kết thúc nếu status="ended" HOẶC endTime đã qua */
    public boolean isEnded() {
        if ("ended".equals(status)) return true;
        if (endTime > 0 && System.currentTimeMillis() >= endTime) return true;
        return false;
    }

    // =========================================================================
    // GETTERS
    // =========================================================================
    public String getId()                 { return id; }
    public String getFirebaseKey()        { return firebaseKey; }
    public String getName()               { return name; }
    public double getCurrentBid()         { return currentBid; }
    public String getTimeRemaining()      { return timeRemaining; }
    public String getImagePath()          { return imagePath; }
    public String getSellerUsername()     { return sellerUsername; }
    public String getCategory()           { return category; }
    public double getStepPrice()          { return stepPrice; }
    public String getStatus()             { return status; }
    public long   getEndTime()            { return endTime; }
    public String getHighestBidder()      { return highestBidder; }
    public String getHighestBidderPhone() { return highestBidderPhone; }
    public String getHighestBidderEmail() { return highestBidderEmail; }

    // =========================================================================
    // SETTERS
    // =========================================================================
    public void setFirebaseKey(String k)         { this.firebaseKey = k; }
    public void setCurrentBid(double v)          { this.currentBid = v; }
    public void setStatus(String s)              { this.status = s; }
    public void setEndTime(long t)               { this.endTime = t; }
    public void setHighestBidder(String v)       { this.highestBidder = v; }
    public void setHighestBidderPhone(String v)  { this.highestBidderPhone = v; }
    public void setHighestBidderEmail(String v)  { this.highestBidderEmail = v; }
}
