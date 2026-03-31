package client.model;

// Lớp Product đóng vai trò như một cái khuôn (Khuôn mẫu dữ liệu)
// Bạn làm Database sẽ lấy dữ liệu từ SQL và đổ vào khuôn này, sau đó gửi cho Giao diện
public class Product {
    
    // Các thuộc tính cơ bản của một món hàng đấu giá
    private String id;              // Mã sản phẩm (để sau này bấm vào còn biết là đang xem cái nào)
    private String name;            // Tên sản phẩm hiển thị trên thẻ
    private double currentBid;      // Giá cao nhất hiện tại (Ví dụ: 950.0)
    private String timeRemaining;   // Thời gian còn lại (Ví dụ: "1h 15m")
    private String imagePath;       // Đường dẫn tới bức ảnh của sản phẩm

    // Hàm khởi tạo (Constructor) dùng để tạo ra một sản phẩm mới với đầy đủ thông tin
    public Product(String id, String name, double currentBid, String timeRemaining, String imagePath) {
        this.id = id;
        this.name = name;
        this.currentBid = currentBid;
        this.timeRemaining = timeRemaining;
        this.imagePath = imagePath;
    }

    // =========================================================
    // CÁC HÀM GETTER: Dùng để lấy thông tin ra vẽ lên giao diện
    // (Bắt buộc phải có vì các biến ở trên đang để chế độ private bảo mật)
    // =========================================================
    public String getId() { return id; }
    public String getName() { return name; }
    public double getCurrentBid() { return currentBid; }
    public String getTimeRemaining() { return timeRemaining; }
    public String getImagePath() { return imagePath; }
}