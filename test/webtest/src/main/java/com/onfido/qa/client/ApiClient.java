package com.onfido.qa.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onfido.qa.client.dto.Applicant;
import com.onfido.qa.client.dto.ApplicantRequest;
import com.onfido.qa.client.dto.WorkflowLink;
import com.onfido.qa.client.dto.WorkflowLinkRequest;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;
import retrofit2.HttpException;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;

import java.io.IOException;

public class ApiClient {

    private final Api api;

    public ApiClient(String baseUrl, String apiToken) {

        var authorizationHeader = "Token token=" + apiToken;

        var builder = new OkHttpClient.Builder().addInterceptor(chain -> {
            Request newRequest = chain.request().newBuilder()
                                      .addHeader("Authorization", authorizationHeader)
                                      .build();
            return chain.proceed(newRequest);
        });

        if (!System.getenv().containsKey("CI")) {
            builder.addInterceptor(new LoggingInterceptor(LoggingInterceptor.Level.BODY));
        }

        var objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        var retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(JacksonConverterFactory.create(objectMapper))
                .client(builder.build())
                .build();

        api = retrofit.create(Api.class);

    }


    public WorkflowLink createWorkflowLink(WorkflowLinkRequest request) {
        return execute(api.createWorkflowLink(request));
    }

    public Applicant createApplicant(ApplicantRequest request) {
        return execute(api.createApplicant(request));
    }

    private interface Api {

        @POST("/v4/workflow_links")
        Call<WorkflowLink> createWorkflowLink(@Body WorkflowLinkRequest request);

        @POST("/v3.4/applicants")
        Call<Applicant> createApplicant(@Body ApplicantRequest request);
    }

    private static <T> T execute(Call<T> call) {
        Response<T> response;

        try {
            response = call.execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (!response.isSuccessful()) {
            throw new HttpException(response);
        }

        return response.body();

    }
}
