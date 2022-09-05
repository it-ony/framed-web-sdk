package com.onfido.qa.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;
import java.util.UUID;

public class WorkflowLink {

    @JsonProperty("id")
    public UUID id;

    @JsonProperty("expires_at")
    public Date expiresAt;

    @JsonProperty("workflow_id")
    public String workflowId;

    @JsonProperty("applicant_id")
    public String applicantId;

    @JsonProperty("url")
    public String url;

}
