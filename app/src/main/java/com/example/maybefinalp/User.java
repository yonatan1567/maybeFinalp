package com.example.maybefinalp;
public class User {
    private String username;
    private int coins;

    public User(String username, int coins) {
        this.username = username;
        this.coins = coins;
    }

    public String getUsername() {
        return username;
    }

    public int getCoins() {
        return coins;
    }
}
