package io.apicurio.rest.client;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DateValue {

    @JsonProperty
    public Date value;

    public DateValue() {
    }

    public DateValue(Date value) {
        this.value = value;
    }

    @JsonProperty
    public Date getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "DateValue{" +
                "value='" + value + '\'' +
                '}';
    }
}
