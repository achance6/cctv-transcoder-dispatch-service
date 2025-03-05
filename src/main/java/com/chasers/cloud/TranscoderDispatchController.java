package com.chasers.cloud;

import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class TranscoderDispatchController {

    private final TranscoderDispatchService transcoderDispatchService;

    public TranscoderDispatchController(TranscoderDispatchService transcoderDispatchService) {
        this.transcoderDispatchService = transcoderDispatchService;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(TranscoderDispatchController.class);

    @Post
    public HttpResponse<String> index(@Body S3EventNotification s3EventNotification) {
        if (s3EventNotification == null || s3EventNotification.getRecords() == null) {
            LOGGER.error("Received empty or invalid S3 event");
            return HttpResponse.badRequest();
        }
        LOGGER.debug("Received request body: {}", s3EventNotification.getRecords().getFirst());

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

        return HttpResponse.ok("Job accepted with ID: " + jobId);
    }
}

