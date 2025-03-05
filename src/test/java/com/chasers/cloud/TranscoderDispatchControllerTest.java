package com.chasers.cloud;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import io.micronaut.function.aws.proxy.payload1.ApiGatewayProxyRequestEventFunction;
import io.micronaut.function.aws.proxy.MockLambdaContext;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest
class TranscoderDispatchControllerTest {
    private static ApiGatewayProxyRequestEventFunction handler;

    private static final Logger LOGGER = LoggerFactory.getLogger(TranscoderDispatchControllerTest.class);

    @BeforeAll
    static void setupSpec() {
        handler = new ApiGatewayProxyRequestEventFunction();
    }

    @AfterAll
    static void cleanupSpec() {
        handler.getApplicationContext().close();
    }

    @Test
    void testHappyPath(ObjectMapper objectMapper) throws IOException {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setPath("/");
        request.setHttpMethod(HttpMethod.POST.toString());


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

        var response = handler.handleRequest(request, new MockLambdaContext());

        assertEquals(HttpStatus.OK.getCode(), response.getStatusCode());
        assertTrue(response.getBody().contains("Job accepted with ID: "));
    }

    @Test
    void testBadRequest() {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setPath("/");
        request.setHttpMethod(HttpMethod.POST.toString());

        request.setBody("");

        var response = handler.handleRequest(request, new MockLambdaContext());

        assertEquals(HttpStatus.BAD_REQUEST.getCode(), response.getStatusCode());
    }
}
