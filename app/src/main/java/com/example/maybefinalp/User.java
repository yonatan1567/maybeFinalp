package com.example.maybefinalp;

public class User {
    private String name;
    private int coins;

    public User(String name, int coins) {
        this.name = name;
        this.coins = coins;
    }

    public String getName() {
        return name;
    }

    public int getCoins() {
        return coins;
    }
}
