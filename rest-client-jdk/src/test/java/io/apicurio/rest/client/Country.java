package io.apicurio.rest.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Country {

    @JsonProperty
    public String name;
    @JsonProperty
    public String capital;

    public Country() {
    }

    public Country(String name, String capital) {
        this.name = name;
        this.capital = capital;
    }

    @JsonProperty
    public String getName() {
        return name;
    }

    @JsonProperty
    public String getCapital() {
        return capital;
    }

    @Override
    public String toString() {
        return "Country{" +
                "name='" + name + '\'' +
                ", capital='" + capital + '\'' +
                '}';
    }
}
