package client.service; 
// Cũng nằm trong service vì đây là logic nghiệp vụ (business logic)
public class AuctionService {
    //kiểm tra xem giá mới có hợp lệ không
    public static boolean isValidBid(double newPrice, double currentBid, double stepPrice) {
        // Điều kiện hợp lệ:
        // giá mới phải >= giá hiện tại + bước giá
        return newPrice >= currentBid + stepPrice;
    }
}