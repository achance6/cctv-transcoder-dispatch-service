package com.chasers.cloud;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import io.micronaut.function.aws.runtime.AbstractMicronautLambdaRuntime;

import java.net.MalformedURLException;

import com.amazonaws.services.lambda.runtime.RequestHandler;
import io.micronaut.core.annotation.Nullable;

public class FunctionLambdaRuntime extends AbstractMicronautLambdaRuntime<S3Event, APIGatewayProxyResponseEvent, S3Event, APIGatewayProxyResponseEvent> {
    public static void main(String[] args) {
        try {
            new FunctionLambdaRuntime().run(args);

        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    @Override
    @Nullable
    protected RequestHandler<S3Event, APIGatewayProxyResponseEvent> createRequestHandler(String... args) {
        return new FunctionRequestHandler();
    }
}
