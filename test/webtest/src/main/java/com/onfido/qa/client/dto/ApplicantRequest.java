package com.onfido.qa.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ApplicantRequest {

    @JsonProperty("first_name")
    public String firstName;

    @JsonProperty("last_name")
    public String lastName;


    public ApplicantRequest(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }
}
