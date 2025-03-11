package com.chasers.cloud;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import io.micronaut.function.aws.MicronautRequestHandler;
import java.io.IOException;

import io.micronaut.http.HttpMethod;
import io.micronaut.http.HttpStatus;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import io.micronaut.serde.ObjectMapper;
import jakarta.inject.Inject;
import java.util.Collections;
public class FunctionRequestHandler extends MicronautRequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    @Inject
    ObjectMapper objectMapper;

    @Override
    public APIGatewayProxyResponseEvent execute(APIGatewayProxyRequestEvent input) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        if (HttpMethod.parse(input.getHttpMethod()) != HttpMethod.POST) {
            response.setStatusCode(HttpStatus.NOT_FOUND.getCode());
            return response;
        }
        try {
            S3EventNotification s3EventNotification = objectMapper.readValue(input.getBody(), S3EventNotification.class);
        } catch (IOException e) {
            response.setStatusCode(HttpStatus.BAD_REQUEST.getCode());
            return response;
        }
        try {
            String json = new String(objectMapper.writeValueAsBytes(Collections.singletonMap("jobId", "")));
            response.setStatusCode(201);
            response.setBody(json);
        } catch (IOException e) {
            response.setStatusCode(500);
        }
        return response;
    }
}
