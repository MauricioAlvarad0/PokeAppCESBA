package com.example.pokeappcesba;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Pokemon {
    public String name;
    public int id;
    public int height;
    public int weight;
    public Sprites sprites;
    public List<TypeSlot> types;
    public List<AbilitySlot> abilities;
    public List<StatSlot> stats;
    public Cries cries;

    public static class TypeSlot {
        public TypeDetail type;
    }

    public static class TypeDetail {
        public String name;
    }

    public static class AbilitySlot {
        public AbilityDetail ability;
    }

    public static class AbilityDetail {
        public String name;
    }

    public static class StatSlot {
        @SerializedName("base_stat")
        public int baseStat;
        public StatDetail stat;
    }

    public static class StatDetail {
        public String name;
    }

    public static class Cries {
        public String latest;
        public String legacy;
    }
}
