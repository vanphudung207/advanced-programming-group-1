package client.model;

public class User {
    private String username;
    private String password;
    private String phone;
    private double balance = 0.0;

    public User() {}

    public User(String username, String password, String phone) {
        this.username = username;
        this.password = password;
        this.phone = phone;
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getPhone() { return phone; }
    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }
}