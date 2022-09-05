package com.onfido.qa.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;
import java.util.UUID;

public class WorkflowLinkRequest {

    @JsonProperty("applicant_id")
    public UUID applicantId;

    @JsonProperty("workflow_id")
    public UUID workflowId;

    @JsonProperty("completed_redirect_url")
    public String completedRedirectUrl;

    @JsonProperty("expires_at")
    public Date expiresAt;

    @JsonProperty("expired_redirect_url")
    public String expiredRedirectUrl;

    @JsonProperty
    public String language;


    public WorkflowLinkRequest(UUID applicantId, UUID workflowId) {
        this.applicantId = applicantId;
        this.workflowId = workflowId;
    }

    public WorkflowLinkRequest completedRedirectUrl(String completedRedirectUrl) {
        this.completedRedirectUrl = completedRedirectUrl;
        return this;
    }

    public WorkflowLinkRequest expiresAt(Date expiresAt) {
        this.expiresAt = expiresAt;
        return this;
    }

    public WorkflowLinkRequest expiredRedirectUrl(String expiredRedirectUrl) {
        this.expiredRedirectUrl = expiredRedirectUrl;
        return this;
    }

    public WorkflowLinkRequest language(String language) {
        this.language = language;
        return this;
    }
}
