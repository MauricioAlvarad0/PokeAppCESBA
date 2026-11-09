package com.example.pokeappcesba.models;

public class BattleRecord {
    public String date;
    public String winnerName;
    public String p1Name;
    public String p1SpriteUrl;
    public String p2Name;
    public String p2SpriteUrl;

    public BattleRecord(String date, String winnerName, String p1Name, String p1SpriteUrl, String p2Name, String p2SpriteUrl) {
        this.date = date;
        this.winnerName = winnerName;
        this.p1Name = p1Name;
        this.p1SpriteUrl = p1SpriteUrl;
        this.p2Name = p2Name;
        this.p2SpriteUrl = p2SpriteUrl;
    }
}