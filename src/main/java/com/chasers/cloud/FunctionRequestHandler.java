package com.chasers.cloud;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import io.micronaut.function.aws.MicronautRequestHandler;
import java.io.IOException;

import io.micronaut.http.HttpMethod;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import io.micronaut.serde.ObjectMapper;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
public class FunctionRequestHandler extends MicronautRequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    @Inject
    private TranscoderDispatchService transcoderDispatchService;

    @Inject
    private ObjectMapper objectMapper;

    private static final Logger LOGGER = LoggerFactory.getLogger(FunctionRequestHandler.class);

    @Override
    public APIGatewayProxyResponseEvent execute(APIGatewayProxyRequestEvent input) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        if (HttpMethod.parse(input.getHttpMethod()) != HttpMethod.POST) {
            response.setStatusCode(HttpStatus.NOT_FOUND.getCode());
            return response;
        }

        S3EventNotification s3EventNotification;
        try {
            s3EventNotification = objectMapper.readValue(input.getBody(), S3EventNotification.class);
        } catch (IOException e) {
            LOGGER.error("Error deserializing input");
            response.setStatusCode(HttpStatus.BAD_REQUEST.getCode());
            return response;
        }
        if (s3EventNotification == null || s3EventNotification.getRecords() == null) {
            LOGGER.error("Received empty or invalid S3 event");
            response.setStatusCode(HttpStatus.BAD_REQUEST.getCode());
            return response;
        }
        LOGGER.info("Received request body: {}", s3EventNotification);
        S3EventNotification.S3EventNotificationRecord record = s3EventNotification.getRecords().getFirst();

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
