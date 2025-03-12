package com.chasers.cloud;

import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.mediaconvert.MediaConvertClient;
import software.amazon.awssdk.services.mediaconvert.model.*;

import java.util.HashMap;
import java.util.Map;

@Singleton
public class TranscoderDispatchService {

    private static final String s3OutputURI = "s3://cctv-transcoded-video-storage/";

    private static final String mediaConvertRoleArn = "arn:aws:iam::442426851957:role/service-role/MediaConvert_Default_Role";

    private static final Logger LOGGER = LoggerFactory.getLogger(TranscoderDispatchService.class);

    public String createMediaJob(S3Event s3Event) {

        LOGGER.info("Received request body: {}", s3Event);
        S3EventNotification.S3EventNotificationRecord record = s3Event.getRecords().getFirst();

        // Extract bucket name and object key
        String bucketName = record.getS3().getBucket().getName();
        String objectKey = record.getS3().getObject().getKey();

        LOGGER.debug("Extracted bucket name: {}", bucketName);
        LOGGER.debug("Extracted object key: {}", objectKey);

        // Construct the ARN
        String s3ObjectARN = String.format("s3://%s/%s", bucketName, objectKey);

        LOGGER.info("Attempting to create media job with ARN: {}", s3ObjectARN);
        CreateJobResponse createJobResponse;
        try (MediaConvertClient mediaConvertClient = MediaConvertClient.create()) {

            String thumbsOutput = s3OutputURI + "thumbs/";
            String mp4Output = s3OutputURI + "mp4/";

            LOGGER.debug("MediaConvert role ARN: {}", mediaConvertRoleArn);
            LOGGER.debug("MediaConvert input file ARN: {}", s3ObjectARN);
            LOGGER.debug("MediaConvert output base path: {}", s3OutputURI);

            OutputGroup fileMp4 = createMp4OutputGroup(mp4Output);
            OutputGroup thumbsGroup = createThumbsOutputGroup(thumbsOutput);

            JobSettings jobSettings = createJobSettings(s3ObjectARN, fileMp4, thumbsGroup);

            CreateJobRequest createJobRequest = CreateJobRequest.builder()
                    .role(mediaConvertRoleArn)
                    .settings(jobSettings)
                    .build();

            createJobResponse = mediaConvertClient.createJob(createJobRequest);
            LOGGER.info("Created MediaConvert job with job ID: {}", createJobResponse.job().id());
        }

        return createJobResponse.job().id();
    }

    private JobSettings createJobSettings(String s3InputFileURI, OutputGroup... outputGroups) {
        Map<String, AudioSelector> audioSelectors = new HashMap<>();
        audioSelectors.put("Audio Selector 1", AudioSelector.builder().defaultSelection(AudioDefaultSelection.DEFAULT).offset(0).build());

        return JobSettings.builder()
                .followSource(1)
                .inputs(
                        Input.builder()
                                .audioSelectors(audioSelectors)
                                .videoSelector(VideoSelector.builder().build())
                                .timecodeSource(InputTimecodeSource.ZEROBASED)
                                .fileInput(s3InputFileURI)
                                .build())
                .outputGroups(outputGroups)
                .build();
    }

    private OutputGroup createMp4OutputGroup(String mp4Destination) {
        return OutputGroup.builder().name("File Group")
                .outputGroupSettings(
                        OutputGroupSettings.builder()
                                .type(OutputGroupType.FILE_GROUP_SETTINGS)
                                .fileGroupSettings(
                                        FileGroupSettings.builder()
                                                .destination(mp4Destination)
                                                .destinationSettings(
                                                        DestinationSettings.builder()
                                                                .s3Settings(
                                                                        S3DestinationSettings.builder()
                                                                                .storageClass(S3StorageClass.STANDARD)
                                                                                .build())
                                                                .build())
                                                .build())
                                .build())
                .outputs(
                        Output.builder().preset("ultra-low-res").nameModifier("-ultra-low-res").build(),
                        Output.builder().preset("low-res").nameModifier("-low-res").build(),
                        Output.builder().preset("medium-res").nameModifier("-medium-res").build(),
                        Output.builder().preset("high-res").nameModifier("-high-res").build()
                ).build();
    }

    private OutputGroup createThumbsOutputGroup(String thumbsOutput) {
        return OutputGroup.builder()
                .name("File Group")
                .customName("thumbs")
                .outputGroupSettings(OutputGroupSettings.builder()
                        .type(OutputGroupType.FILE_GROUP_SETTINGS)
                        .fileGroupSettings(FileGroupSettings.builder()
                                .destination(thumbsOutput).build())
                        .build())
                .outputs(Output.builder().extension("jpg")
                        .containerSettings(ContainerSettings.builder()
                                .container(ContainerType.RAW).build())
                        .videoDescription(VideoDescription.builder()
                                .height(480)
                                .scalingBehavior(ScalingBehavior.DEFAULT)
                                .sharpness(50).antiAlias(AntiAlias.ENABLED)
                                .timecodeInsertion(
                                        VideoTimecodeInsertion.DISABLED)
                                .colorMetadata(ColorMetadata.INSERT)
                                .dropFrameTimecode(DropFrameTimecode.ENABLED)
                                .codecSettings(VideoCodecSettings.builder()
                                        .codec(VideoCodec.FRAME_CAPTURE)
                                        .frameCaptureSettings(
                                                FrameCaptureSettings
                                                        .builder()
                                                        .framerateNumerator(1)
                                                        .framerateDenominator(1)
                                                        .maxCaptures(1)
                                                        .quality(80)
                                                        .build())
                                        .build())
                                .build())
                        .build())
                .build();
    }
}
