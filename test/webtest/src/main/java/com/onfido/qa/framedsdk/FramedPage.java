package com.onfido.qa.framedsdk;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onfido.qa.webdriver.Driver;
import com.onfido.qa.webdriver.common.Page;
import org.openqa.selenium.By;

import java.util.Optional;

public class FramedPage extends Page {

    public static final By FRAME = By.cssSelector("iframe");
    public static final String ERROR_HANDLER = "(e) => { console.error('init_error', e); window.init_error = e; }";
    public static final By ID = By.id("frame-test");

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

    @Override
    protected void verifyPage(Driver driver) {
        driver.waitFor.visibility(ID);
    }

    public static class Error {

        @JsonProperty
        public String message;
    }
}
