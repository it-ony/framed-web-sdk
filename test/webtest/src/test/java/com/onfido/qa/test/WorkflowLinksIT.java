package com.onfido.qa.test;

import com.onfido.qa.Region;
import com.onfido.qa.client.dto.Applicant;
import com.onfido.qa.client.dto.ApplicantRequest;
import com.onfido.qa.client.dto.WorkflowLinkRequest;
import com.onfido.qa.framedsdk.FramedPage;
import com.onfido.qa.websdk.page.Complete;
import com.onfido.qa.websdk.page.UserConsent;
import com.onfido.qa.websdk.page.Welcome;
import com.onfido.sdk.Raw;
import org.openqa.selenium.JavascriptException;
import org.openqa.selenium.TimeoutException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Locale;
import java.util.UUID;

import static com.onfido.qa.Region.CA;
import static com.onfido.qa.Region.EU;
import static com.onfido.qa.Region.US;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SuppressWarnings("OverlyCoupledMethod")
public class WorkflowLinksIT extends FramedWebSdkIT {

    public static final Raw RAW = new Raw("(data) => {window.onCompleteData = data}");

    @DataProvider
    public static Object[][] regions() {
        return new Object[][]{
                {EU}, {US}, {CA}
        };
    }


    @Test(description = "NoRegionLeadsToError")
    public void testNoRegionLeadsToError() {

        assertThatThrownBy(() -> {
            onfido()
                    .withWorkflowLinkId(UUID.randomUUID())
                    .init();
        })
                .isInstanceOf(JavascriptException.class)
                .hasMessageContaining("'workflowLinkId' required to provide the 'region' as well");
    }

    @Test(description = "happy path of workflowLinks", dataProvider = "regions")
    public void testHappyPathOfWorkflowLinks(Region region) throws Exception {

        var api = api(region);
        Applicant applicant = api.createApplicant(new ApplicantRequest(faker.name().firstName(), faker.name().lastName()));
        var workflowLink = api.createWorkflowLink(new WorkflowLinkRequest(applicant.id, workflowId(region)));

        var framedPage = onfido()
                .withWorkflowLinkId(workflowLink.id)
                .withRegion(region)
                .withOnComplete(RAW)
                .init();

        var src = framedPage.frameSrc();

        assertThat(src).contains(String.format("sdk.%s.onfido.app", region.name().toLowerCase(Locale.ROOT)));
        assertThat(src).contains(workflowLink.id.toString());

        var driver = driver();

        framedPage.inner(() -> {
            new Welcome(driver).continueToNextStep();

            try {
                new UserConsent(driver).acceptUserConsent(null);
            } catch (TimeoutException ignored) {
                // user consent screen is showing up, depending on the location of the browser
            }

            // wait for the complete screen
            new Complete(driver);
        });

        var completeData = framedPage.getCompleteData();
        assertThat(completeData).isNotNull();

    }

    protected UUID workflowId(Region region) {
        return UUID.fromString(properties().getProperty("workflowId." + region.name().toLowerCase(Locale.ROOT)));
    }

    @Test(description = "error is invoked, if workflow link id doesn't exist")
    public void testErrorIsInvokedIfWorkflowLinkIdNotExist() {
        var framedPage = onfido()
                .withWorkflowLinkId(UUID.randomUUID())
                .withRegion(EU)
                .withErrorHandler(FramedPage.ERROR_HANDLER)
                .init();

        driver().waitFor.waitFor((o) -> {
                return framedPage.getError().isPresent();
            }
        );

        var error = framedPage.getError();
        assertThat(error).isPresent();
        assertThat(error.get().message).contains("Session or Workflow not found");

    }
}
