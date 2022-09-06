package com.onfido.qa.framedsdk;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onfido.qa.webdriver.Driver;
import com.onfido.qa.webdriver.common.Page;
import com.onfido.qa.websdk.model.CompleteData;
import org.openqa.selenium.By;

import java.util.Optional;

public class FramedPage extends Page {

    public static final By FRAME = By.cssSelector("iframe");
    public static final String ERROR_HANDLER = "(e) => { console.error('init_error', e); window.init_error = e; }";
    public static final By ID = By.id("frame-test");

    final static ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }


    public FramedPage(Driver driver) {
        super(driver);
    }

    public String frameSrc() {
        return driver.waitFor.clickable(FRAME).getAttribute("src");
    }

    public Optional<Error> getError() {

        driver.waitFor.invisible(FRAME);

        var error = driver.executeScript("return JSON.stringify(window.init_error)");

        if (error == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(new ObjectMapper().readValue((String) error, Error.class));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T inner(FrameExecutor<T> executor) throws Exception {
        return executeWithinFrame(FRAME, executor);
    }

    public void inner(FrameExecutorVoid runnable) throws Exception {
        executeWithinFrame(FRAME, runnable);
    }

    public CompleteData getCompleteData() throws JsonProcessingException {
        var json = (String) driver.executeScript("return JSON.stringify(window.onCompleteData)");
        return objectMapper.readValue(json, CompleteData.class);

    }

    @Override
    protected void verifyPage(Driver driver) {
        driver.waitFor.clickable(ID);
    }

    public static class Error {

        @JsonProperty
        public String message;
    }
}
