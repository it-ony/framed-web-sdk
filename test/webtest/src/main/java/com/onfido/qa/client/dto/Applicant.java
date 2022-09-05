package com.onfido.qa.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public class Applicant {

    @JsonProperty
    public UUID id;

    @JsonProperty
    public Boolean sandbox;

    @JsonProperty("first_name")
    public String firstName;

    @JsonProperty("last_name")
    public String lastName;

}
