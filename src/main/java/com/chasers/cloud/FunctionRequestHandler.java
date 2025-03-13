package com.chasers.cloud;

import com.amazonaws.services.lambda.runtime.events.S3Event;
import io.micronaut.function.aws.MicronautRequestHandler;

import java.io.IOException;

import io.micronaut.http.HttpStatus;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import io.micronaut.json.JsonMapper;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

public class FunctionRequestHandler extends MicronautRequestHandler<S3Event, APIGatewayProxyResponseEvent> {

    private static final TranscoderDispatchService transcoderDispatchService = new TranscoderDispatchService();

    private static final Logger LOGGER = LoggerFactory.getLogger(FunctionRequestHandler.class);

    @Inject
    JsonMapper objectMapper;

    @Override
    public APIGatewayProxyResponseEvent execute(S3Event s3Event) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();

        if (s3Event == null || s3Event.getRecords() == null || s3Event.getRecords().isEmpty()) {
            LOGGER.error("Received empty or invalid S3 event");
            response.setStatusCode(HttpStatus.BAD_REQUEST.getCode());
            return response;
        }

        String jobId = transcoderDispatchService.createMediaJob(s3Event);

        try {
            String json = new String(objectMapper.writeValueAsBytes(Collections.singletonMap("jobId", jobId)));
            response.setStatusCode(HttpStatus.OK.getCode());
            response.setBody(json);
        } catch (IOException e) {
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.getCode());
        }
        return response;
    }
}
