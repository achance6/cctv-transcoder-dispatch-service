package com.chasers.cloud;

import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.function.aws.runtime.AbstractMicronautLambdaRuntime;
import io.micronaut.http.HttpResponse;
import io.micronaut.serde.annotation.SerdeImport;

import java.net.MalformedURLException;

@SerdeImport(HttpResponse.class)
public class FunctionLambdaRuntime extends AbstractMicronautLambdaRuntime<S3Event, HttpResponse<String>, S3Event, HttpResponse<String>> {
    public static void main(String[] args) {
        try {
            new FunctionLambdaRuntime().run(args);

        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    @Override
    @Nullable
    protected RequestHandler<S3Event, HttpResponse<String>> createRequestHandler(String... args) {
        return new FunctionRequestHandler();
    }
}
