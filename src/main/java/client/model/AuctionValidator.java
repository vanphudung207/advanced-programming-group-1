package client.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class AuctionValidator {

    private static final DateTimeFormatter FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // =============================================
    // Is the bid amount valid?
    // Must be at least currentBid + stepPrice
    // =============================================
    public static boolean isBidValid(AuctionItem item, double bidAmount) {
        double minimum = item.getCurrentBid() + item.getStepPrice();
        return bidAmount >= minimum;
    }

    // =============================================
    // Is the auction still open?
    // =============================================
    public static boolean isAuctionOpen(AuctionItem item) {
    if (item.isClosed()) return false;
    return System.currentTimeMillis() < item.getEndTime();
    }

    // =============================================
    // Can this user bid? (not the seller)
    // =============================================
    public static boolean canUserBid(AuctionItem item, String username) {
        return !item.getSellerUsername().equals(username);
    }

    // =============================================
    // Login check
    // =============================================
    public static boolean checkLogin(String username, String password) {
        for (User u : DataManager.loadUsers()) {
            if (u.getUsername().equals(username) && u.getPassword().equals(password)) {
                return true;
            }
        }
        return false;
    }

    // =============================================
    // Full bid attempt — returns a message string
    // Controllers can show this message directly
    // =============================================
    public static String placeBid(String itemId, String bidderUsername, double bidAmount) {
        // Load current data
        Map<String, AuctionItem> items = DataManager.loadItems();
        AuctionItem target = items.get(itemId);

        if (target == null)           return "Sản phẩm không tồn tại!";
        if (!isAuctionOpen(target))   return "Phiên đấu giá đã kết thúc!";
        if (!canUserBid(target, bidderUsername))
                                      return "Bạn không thể tự đấu giá hàng của mình!";
        if (!isBidValid(target, bidAmount))
                                      return "Giá phải cao hơn ít nhất "
                                          + String.format("%,.0f VNĐ", target.getCurrentBid() + target.getStepPrice());

        // All checks passed — update and save
        target.setCurrentBid(bidAmount);
        target.setHighestBidder(bidderUsername);

        // Grab their phone number too
        String phone = DataManager.loadUsers().stream()
            .filter(u -> u.getUsername().equals(bidderUsername))
            .map(u -> u.getPhone())
            .findFirst().orElse("Không có");
        target.setHighestBidderPhone(phone);
        DataManager.saveItems(items);

        Bid bid = new Bid(itemId, bidderUsername, bidAmount);
        java.util.List<Bid> bids = DataManager.loadBids();
        bids.add(bid);
        DataManager.saveBids(bids);

        return "OK:" + String.format("%,.0f VNĐ", bidAmount);
    }
}