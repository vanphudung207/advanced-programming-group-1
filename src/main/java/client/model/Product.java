package client.model;

public class Product {
    private String id;
    private String name;
    private double currentBid;
    private String timeRemaining;
    private String imagePath;
    
    // =========================================================
    // MỚI THÊM: Biến lưu tên tài khoản của người đã đăng món hàng này
    // =========================================================
    private String sellerUsername; 

    // Cập nhật lại Hàm khởi tạo (Thêm tham số sellerUsername ở cuối)
    public Product(String id, String name, double currentBid, String timeRemaining, String imagePath, String sellerUsername) {
        this.id = id;
        this.name = name;
        this.currentBid = currentBid;
        this.timeRemaining = timeRemaining;
        this.imagePath = imagePath;
        this.sellerUsername = sellerUsername; // Gắn mác người bán
    }

    // Các hàm Getter để lấy dữ liệu ra
    public String getId() { return id; }
    public String getName() { return name; }
    public double getCurrentBid() { return currentBid; }
    public String getTimeRemaining() { return timeRemaining; }
    public String getImagePath() { return imagePath; }
    
    // Hàm mới để lấy ra tên người bán
    public String getSellerUsername() { return sellerUsername; } 
}