package client.model;

public class Product {
    private String id;
    private String name;
    private double currentBid;
    private String timeRemaining;
    private String imagePath;
    private String sellerUsername; // Người bán
    private String category;       // Danh mục sản phẩm (Điện tử, Gia dụng...)

    // =========================================================
    // CONSTRUCTOR 1: Hàm khởi tạo ĐẦY ĐỦ (Dùng khi tạo sản phẩm mới)
    // =========================================================
    public Product(String id, String name, double currentBid, String timeRemaining, String imagePath, String sellerUsername, String category) {
        this.id = id;
        this.name = name;
        this.currentBid = currentBid;
        this.timeRemaining = timeRemaining;
        this.imagePath = imagePath;
        this.sellerUsername = sellerUsername; 
        this.category = category; 
    }

    // =========================================================
    // CONSTRUCTOR 2: Hàm khởi tạo RÚT GỌN (Giữ lại để code cũ không bị lỗi)
    // Nếu ai đó không truyền danh mục vào, hệ thống tự hiểu là "Khác"
    // =========================================================
    public Product(String id, String name, double currentBid, String timeRemaining, String imagePath, String sellerUsername) {
        this(id, name, currentBid, timeRemaining, imagePath, sellerUsername, "Khác");
    }

    // =========================================================
    // CÁC HÀM GETTER ĐỂ LẤY DỮ LIỆU RA
    // =========================================================
    public String getId() { return id; }
    public String getName() { return name; }
    public double getCurrentBid() { return currentBid; }
    public String getTimeRemaining() { return timeRemaining; }
    public String getImagePath() { return imagePath; }
    public String getSellerUsername() { return sellerUsername; } 
    
    // ĐÃ THÊM: Hàm lấy tên danh mục để làm chức năng bộ lọc
    public String getCategory() { return category; } 
}