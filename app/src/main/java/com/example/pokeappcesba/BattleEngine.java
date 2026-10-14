package com.example.pokeappcesba;

import java.util.Random;

public class BattleEngine {
    
    public static final int[] LEGENDARY_IDS = {144, 145, 146, 150, 151, 243, 244, 245, 249, 250, 377, 378, 379, 380, 381, 382, 383, 384};

    public static class BattlePokemon {
        public Pokemon pokemon;
        public int maxHp;
        public int currentHp;
        public int attack;
        public int defense;
        public int speed;
        public java.util.List<String> moves = new java.util.ArrayList<>();

        public BattlePokemon(Pokemon p) {
            this.pokemon = p;
            
            // Buscar stats bases
            int baseHp = 50, baseAtk = 50, baseDef = 50, baseSpd = 50;
            if (p.stats != null) {
                for (Pokemon.StatSlot s : p.stats) {
                    if (s.stat.name.equals("hp")) baseHp = s.baseStat;
                    else if (s.stat.name.equals("attack")) baseAtk = s.baseStat;
                    else if (s.stat.name.equals("defense")) baseDef = s.baseStat;
                    else if (s.stat.name.equals("speed")) baseSpd = s.baseStat;
                }
            }
            
            this.maxHp = baseHp * 3;
            this.currentHp = this.maxHp;
            this.attack = baseAtk;
            this.defense = baseDef;
            this.speed = baseSpd;

            // Pick 4 random moves or first 4
            if (p.moves != null && !p.moves.isEmpty()) {
                Random rand = new Random();
                for (int i = 0; i < 4; i++) {
                    if (p.moves.size() > 0) {
                        String moveName = p.moves.get(rand.nextInt(p.moves.size())).move.name;
                        if (!moves.contains(moveName.toUpperCase())) {
                            moves.add(moveName.toUpperCase());
                        } else {
                            // If duplicate, try to find another, or just let it loop
                            if (moves.size() < p.moves.size()) {
                                i--; // try again
                            } else {
                                break;
                            }
                        }
                    }
                }
            }
            if (moves.isEmpty()) {
                moves.add("PLACAJE");
            }
        }

        public boolean isFainted() {
            return currentHp <= 0;
        }
    }

    public static boolean determineFirstAttacker(BattlePokemon p1, BattlePokemon p2) {
        if (p1.speed == p2.speed) {
            return new Random().nextBoolean();
        }
        return p1.speed > p2.speed; // True si p1 ataca primero
    }

    public static int calculateDamage(int attack, int defense) {
        Random rand = new Random();
        int baseDamage = (attack * 15) / Math.max(1, defense);
        int variation = rand.nextInt(6) + 1; // 1 a 6
        return Math.max(1, baseDamage + variation);
    }

    public static String getRandomMove(BattlePokemon bp) {
        if (bp == null || bp.moves == null || bp.moves.isEmpty()) {
            return "PLACAJE";
        }
        Random rand = new Random();
        return bp.moves.get(rand.nextInt(bp.moves.size()));
    }
}
