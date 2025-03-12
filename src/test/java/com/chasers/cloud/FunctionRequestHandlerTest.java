package com.chasers.cloud;

import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import io.micronaut.http.HttpResponse;
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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

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
    public void testHandler(ObjectMapper objectMapper) {

        S3EventNotification.S3EventNotificationRecord s3Event = new S3EventNotification.S3EventNotificationRecord(
                Region.US_EAST_1.toString(),
                "ObjectCreated:Put",
                "aws:s3",
                ZonedDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                "2.1",
                new S3EventNotification.RequestParametersEntity("127.0.0.1"),
                new S3EventNotification.ResponseElementsEntity("C3D13FE58DE4C810", "FMyUVURIY8/IgAtTv8xRjskZQpcIZ9KG4V5Wp6S7S/JRWeUWerMUE5JgHvANOjpD"),
                new S3EventNotification.S3Entity("testConfigRule", new S3EventNotification.S3BucketEntity("cctv-video-storage", new S3EventNotification.UserIdentityEntity("A3NL1KOZZKExample"), "arn:aws:s3:::cctv-video-storage"),
                        new S3EventNotification.S3ObjectEntity(URLEncoder.encode("Me at the zoo.mp4", StandardCharsets.UTF_8), 1100L, "4e088404aece61e07e7cfc8752927f35", "gKpkHSFzm.3lnBK.vAADCoqwAPiMFsOA", "0055AED6DCD90281E5"),
                        "1.0"),
                new S3EventNotification.UserIdentityEntity("AIDAJDPLRKLG7UEXAMPLE")
        );

        S3Event eventNotification = new S3Event(List.of(s3Event));

        HttpResponse<String> response = handler.execute(eventNotification);

        String regex = "[0-9]{12,}-[a-zA-Z0-9]{6}";

        Pattern pattern = Pattern.compile(regex);

        Map<?, ?> responseObject = null;
        try {
            responseObject = objectMapper.readValue(response.getBody().orElseThrow(), Map.class);
        } catch (IOException e) {
            LOGGER.error("Error parsing response");
        }
        assertNotNull(responseObject);

        assertEquals(HttpStatus.OK, response.getStatus());
        assertTrue(pattern.matcher((String) responseObject.get("jobId")).matches(), "Response does not match expected format, response was: " + responseObject);
    }

    @Test
    public void testBadRequest() {
        S3Event request = new S3Event();

        var response = handler.execute(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatus());
    }
}
