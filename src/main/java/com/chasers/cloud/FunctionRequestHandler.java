package com.chasers.cloud;

import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
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
        LOGGER.info("Received request body: {}", s3Event);
        S3EventNotification.S3EventNotificationRecord record = s3Event.getRecords().getFirst();

        // Extract bucket name and object key
        String bucketName = record.getS3().getBucket().getName();
        String objectKey = record.getS3().getObject().getKey();

        LOGGER.debug("Extracted bucket name: {}", bucketName);
        LOGGER.debug("Extracted object key: {}", objectKey);

        // Construct the ARN
        String s3ObjectARN = String.format("s3://%s/%s", bucketName, objectKey);

        LOGGER.info("Attempting to create media job with URI: {}", s3ObjectARN);
        String jobId = transcoderDispatchService.createMediaJob(s3ObjectARN);

        try {
            String json = new String(objectMapper.writeValueAsBytes(Collections.singletonMap("jobId", jobId)));
            response.setStatusCode(201);
            response.setBody(json);
        } catch (IOException e) {
            response.setStatusCode(500);
        }
        return response;
    }
}
