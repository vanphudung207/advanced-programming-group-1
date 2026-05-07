package client.service;

public class AuctionService {

    /**
     * Kiểm tra giá mới có hợp lệ không.
     * Điều kiện: newPrice >= currentBid + stepPrice
     *
     * @param newPrice   Giá người dùng vừa nhập
     * @param currentBid Giá đang cao nhất hiện tại
     * @param stepPrice  Bước giá tối thiểu mỗi lần trả
     * @return true nếu hợp lệ, false nếu không
     */
    public static boolean isValidBid(double newPrice, double currentBid, double stepPrice) {
        return newPrice >= currentBid + stepPrice;
    }
}
