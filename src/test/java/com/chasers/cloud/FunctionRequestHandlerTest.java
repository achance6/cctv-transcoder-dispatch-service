package com.chasers.cloud;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import io.micronaut.http.HttpMethod;
import io.micronaut.http.HttpStatus;
import io.micronaut.serde.ObjectMapper;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest
public class FunctionRequestHandlerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(FunctionRequestHandlerTest.class);
    private static FunctionRequestHandler handler;

    @BeforeAll
    public static void setupServer() {
        handler = new FunctionRequestHandler();
    }

    @AfterAll
    public static void stopServer() {
        if (handler != null) {
            handler.getApplicationContext().close();
        }
    }

    @Test
    public void testHandler(ObjectMapper objectMapper) throws IOException {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod(HttpMethod.POST.toString());
        request.setPath("/");

        S3EventNotification.S3EventNotificationRecord s3Event = new S3EventNotification.S3EventNotificationRecord(
                Region.US_EAST_1.toString(),
                "ObjectCreated:Put",
                "aws:s3",
                ZonedDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                "2.1",
                new S3EventNotification.RequestParametersEntity("127.0.0.1"),
                new S3EventNotification.ResponseElementsEntity("C3D13FE58DE4C810", "FMyUVURIY8/IgAtTv8xRjskZQpcIZ9KG4V5Wp6S7S/JRWeUWerMUE5JgHvANOjpD"),
                new S3EventNotification.S3Entity("testConfigRule", new S3EventNotification.S3BucketEntity("cctv-video-storage", new S3EventNotification.UserIdentityEntity("A3NL1KOZZKExample"), "arn:aws:s3:::cctv-video-storage"),
                        new S3EventNotification.S3ObjectEntity("Me at the zoo.mp4", 1100L, "4e088404aece61e07e7cfc8752927f35", "gKpkHSFzm.3lnBK.vAADCoqwAPiMFsOA", "0055AED6DCD90281E5"),
                        "1.0"),
                new S3EventNotification.UserIdentityEntity("AIDAJDPLRKLG7UEXAMPLE")
        );

        S3EventNotification eventNotification = new S3EventNotification(List.of(s3Event));

        String s3EventJson = objectMapper.writeValueAsString(eventNotification);
        LOGGER.info("Sending request: {}", eventNotification);
        request.setBody(s3EventJson);

        APIGatewayProxyResponseEvent response = handler.execute(request);

        String regex = "[0-9]{12,}-[a-zA-Z0-9]{6}";

        Pattern pattern = Pattern.compile(regex);

        @SuppressWarnings("unchecked")
        Map<String, String> responseObject = objectMapper.readValue(response.getBody(), Map.class);

        assertEquals(HttpStatus.CREATED.getCode(), response.getStatusCode());
        assertTrue(pattern.matcher(responseObject.get("jobId")).matches());
    }

    @Test
    public void testBadRequest() {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setPath("/");
        request.setHttpMethod(HttpMethod.POST.toString());

        request.setBody("");

        var response = handler.execute(request);

        assertEquals(HttpStatus.BAD_REQUEST.getCode(), response.getStatusCode());
    }

    @Test
    public void testGetRequest() {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setPath("/");
        request.setHttpMethod(HttpMethod.GET.toString());

        request.setBody("");

        var response = handler.execute(request);

        assertEquals(HttpStatus.NOT_FOUND.getCode(), response.getStatusCode());
    }
}
