package com.chasers.cloud;

import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;

import java.util.Collections;
import java.util.Map;

@Controller
public class TranscoderDispatchController {

    private final TranscoderDispatchService transcoderDispatchService;

    public TranscoderDispatchController(TranscoderDispatchService transcoderDispatchService) {
        this.transcoderDispatchService = transcoderDispatchService;
    }

    @Post
    public Map<String, Object> index(@Body String s3FileLocation) {
        String jobId = transcoderDispatchService.createMediaJob(s3FileLocation);
        return Collections.singletonMap("id", jobId);
    }
}

