package com.example.pokeappcesba;

import com.google.gson.annotations.SerializedName;

public class Sprites {
    @SerializedName("front_default")
    public String frontDefault;

    @SerializedName("back_default")
    public String backDefault;

    @SerializedName("front_shiny")
    public String frontShiny;

    @SerializedName("back_shiny")
    public String backShiny;

    public Other other;

    public static class Other {
        public Showdown showdown;
    }

    public static class Showdown {
        @SerializedName("front_default")
        public String frontDefault;
        @SerializedName("back_default")
        public String backDefault;
        @SerializedName("front_shiny")
        public String frontShiny;
        @SerializedName("back_shiny")
        public String backShiny;
    }
}
