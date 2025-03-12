package com.chasers.cloud;

import com.amazonaws.services.lambda.runtime.events.S3Event;
import io.micronaut.function.aws.MicronautRequestHandler;
import io.micronaut.http.HttpResponse;
import io.micronaut.json.JsonMapper;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;

public class FunctionRequestHandler extends MicronautRequestHandler<S3Event, HttpResponse<String>> {

    private static final TranscoderDispatchService transcoderDispatchService = new TranscoderDispatchService();

    private static final Logger LOGGER = LoggerFactory.getLogger(FunctionRequestHandler.class);

    @Inject
    JsonMapper objectMapper;

    @Override
    public HttpResponse<String> execute(S3Event s3Event) {

        if (s3Event == null || s3Event.getRecords() == null || s3Event.getRecords().isEmpty()) {
            LOGGER.error("Received empty or invalid S3 event");
            return HttpResponse.badRequest();
        }

        String jobId = transcoderDispatchService.createMediaJob(s3Event);

        try {
            String json = new String(objectMapper.writeValueAsBytes(Collections.singletonMap("jobId", jobId)));
            return HttpResponse.ok(json);
        } catch (IOException e) {
            return HttpResponse.serverError();
        }
    }
}
