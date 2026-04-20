package client.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class DataManager {

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    // File paths — Member 1 can replace these with Firebase later
    private static final String USERS_FILE    = "data/users.json";
    private static final String ITEMS_FILE    = "data/items.json";
    private static final String BIDS_FILE     = "data/bids.json";

    // =============================================
    // USERS
    // =============================================
    public static List<User> loadUsers() {
        return loadList(USERS_FILE, new TypeToken<List<User>>(){}.getType());
    }

    public static void saveUsers(List<User> users) {
        saveList(USERS_FILE, users);
    }

    // =============================================
    // AUCTION ITEMS
    // =============================================
    public static List<AuctionItem> loadItems() {
        return loadList(ITEMS_FILE, new TypeToken<List<AuctionItem>>(){}.getType());
    }

    public static void saveItems(List<AuctionItem> items) {
        saveList(ITEMS_FILE, items);
    }

    // =============================================
    // BIDS
    // =============================================
    public static List<Bid> loadBids() {
        return loadList(BIDS_FILE, new TypeToken<List<Bid>>(){}.getType());
    }

    public static void saveBids(List<Bid> bids) {
        saveList(BIDS_FILE, bids);
    }

    // =============================================
    // PRIVATE HELPERS
    // =============================================
    private static <T> List<T> loadList(String path, Type type) {
        File file = new File(path);
        if (!file.exists()) return new ArrayList<>();
        try (Reader reader = new FileReader(file)) {
            List<T> result = gson.fromJson(reader, type);
            return result != null ? result : new ArrayList<>();
        } catch (IOException e) {
            System.err.println("Read error [" + path + "]: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private static <T> void saveList(String path, List<T> list) {
        // Auto-create the data/ folder if it doesn't exist
        new File("data").mkdirs();
        try (Writer writer = new FileWriter(path)) {
            gson.toJson(list, writer);
        } catch (IOException e) {
            System.err.println("Write error [" + path + "]: " + e.getMessage());
        }
    }
}