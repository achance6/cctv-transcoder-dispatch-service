package com.chasers.cloud;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import io.micronaut.function.aws.proxy.payload1.ApiGatewayProxyRequestEventFunction;
import io.micronaut.function.aws.proxy.MockLambdaContext;
import io.micronaut.http.HttpMethod;
import io.micronaut.http.HttpStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HomeControllerTest {
    private static ApiGatewayProxyRequestEventFunction handler;

    @BeforeAll
    static void setupSpec() {
        handler = new ApiGatewayProxyRequestEventFunction();
    }
    @AfterAll
    static void cleanupSpec() {
        handler.getApplicationContext().close();
    }

    @Test
    void testHandler() {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setPath("/");
        request.setHttpMethod(HttpMethod.POST.toString());
        request.setBody("s3://cctv-video-storage/Me at the zoo.mp4");
        var response = handler.handleRequest(request, new MockLambdaContext());

        assertEquals(HttpStatus.OK.getCode(), response.getStatusCode());
        assertTrue(response.getBody().contains("id"));
    }
}
