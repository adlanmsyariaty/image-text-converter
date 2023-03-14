package com.digitalsawitpro.service;

import com.digitalsawitpro.constants.Constants;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class GoogleVisionService {

    public static ImageAnnotatorSettings getImageAnnotatorSettings(Credentials credentials) {
        ImageAnnotatorSettings settings;
        try {
            settings = ImageAnnotatorSettings.newBuilder().setCredentialsProvider(
                    FixedCredentialsProvider.create(credentials)).build();
            return settings;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static ImageAnnotatorClient getImageAnnotatorClient(ImageAnnotatorSettings settings) {
        ImageAnnotatorClient client;
        try {
            client = ImageAnnotatorClient.create(settings);
            return client;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Credentials getGoogleVisionCredentials() throws IOException {
        return GoogleCredentials.fromStream(new FileInputStream(Constants.CREDENTIALS_FILE_PATH));
    }

    public static String extractTextFromImage(Path imagePath, ImageAnnotatorClient client) {
        ByteString imageBytes;
        try {
            imageBytes = ByteString.copyFrom(Files.readAllBytes(imagePath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Feature feature = Feature.newBuilder().setType(Feature.Type.TEXT_DETECTION).build();
        AnnotateImageRequest request =
                AnnotateImageRequest.newBuilder()
                        .addFeatures(feature)
                        .setImage(Image.newBuilder().setContent(imageBytes).build())
                        .build();
        List<AnnotateImageRequest> annotateImageRequestList = new ArrayList<>();
        annotateImageRequestList.add(request);
        BatchAnnotateImagesResponse responses = client.batchAnnotateImages(
                annotateImageRequestList);
        AnnotateImageResponse response = responses.getResponses(0);

        return response.getTextAnnotationsList().get(0).getDescription();
    }
}
