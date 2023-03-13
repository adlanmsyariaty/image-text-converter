package com.digitalsawitpro;

import com.digitalsawitpro.constants.Constants;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.ImageAnnotatorSettings;
import com.google.protobuf.ByteString;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) throws IOException, GeneralSecurityException {
        //GOOGLE DRIVE API
        InputStream inputStream = Main.class.getResourceAsStream(Constants.GOOGLE_OAUTH_PATH);
        if (inputStream == null) {
            throw new FileNotFoundException("Credential file not found: " +
                    Constants.GOOGLE_OAUTH_PATH);
        }
        GoogleClientSecrets googleClientSecrets = GoogleClientSecrets.load(Constants.JSON_FACTORY,
                new InputStreamReader(inputStream));

        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, Constants.JSON_FACTORY, googleClientSecrets, Constants.SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File("tokens")))
                .setAccessType("offline")
                .build();

        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8080).build();
        Credential credential = new AuthorizationCodeInstalledApp(flow, receiver)
                .authorize("user");

        Drive service = new Drive.Builder(HTTP_TRANSPORT, Constants.JSON_FACTORY, credential)
                .setApplicationName(Constants.APPLICATION_NAME)
                .build();

        String queryToFindFolderId = "mimeType='application/vnd.google-apps.folder' and name='" +
                Constants.FOLDER_NAME_IN_GDRIVE + "' and trashed = false";
        FileList folderTargetList = service.files().list()
                .setQ(queryToFindFolderId)
                .setFields("nextPageToken, files(id)")
                .execute();
        String folderId = folderTargetList.getFiles().get(0).getId();

        if (folderId == null) {
            System.out.println("Folder not found: " + Constants.FOLDER_NAME_IN_GDRIVE);
            return;
        }

        String queryToFindFilesByFolderId = "parents='" + folderId + "'";
        FileList result = service.files().list()
                .setPageSize(10)
                .setQ(queryToFindFilesByFolderId)
                .setFields("nextPageToken, files(id, name)")
                .execute();
        List<File> files = result.getFiles();
        List<String> imageFileNameList = new ArrayList<>();
        if (files == null || files.isEmpty()) {
            throw new FileNotFoundException();
        } else {
            for (File file : files) {
                imageFileNameList.add(file.getName());
                OutputStream outputStream = new FileOutputStream(file.getName());
                service.files().get(file.getId()).executeMediaAndDownloadTo(outputStream);
            }
        }

        //CREATE NEW FOLDER
        String folderPath = Constants.IMAGE_FOLDER_PATH;
        createNewFolderAndCopyFile(folderPath, imageFileNameList);

        //GOOGLE VISION API
        Credentials credentials = GoogleCredentials.fromStream(new FileInputStream(
                Constants.CREDENTIALS_FILE_PATH));

        ImageAnnotatorSettings settings = null;
        try {
            settings = ImageAnnotatorSettings.newBuilder()
                    .setCredentialsProvider(FixedCredentialsProvider.create(credentials)).build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        ImageAnnotatorClient client = null;
        try {
            client = ImageAnnotatorClient.create(settings);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ImageAnnotatorClient finalClient = client;

        List<String> englishParagraphList = new ArrayList<>();
        List<String> chineseParagraphList = new ArrayList<>();

        imageFileNameList.stream().forEach(imageFileName -> {
            Path imagePath = Paths.get(Constants.IMAGE_FOLDER_PATH + "/" + imageFileName);

            ByteString imageBytes = null;
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
            BatchAnnotateImagesResponse responses = finalClient.batchAnnotateImages(
                    annotateImageRequestList);
            AnnotateImageResponse response = responses.getResponses(0);

            String fileContent = response.getTextAnnotationsList().get(0).getDescription();
            String[] fileContentArray = fileContent.split(Constants.NEWLINE_SYMBOL);

            boolean isContainsChinese = false;
            for (var i = 0; i < fileContentArray.length; i++) {
                if (!isContainsChinese) {
                    isContainsChinese = isContainsChineseCharacter(fileContentArray[i]);
                }

                String[] subText = fileContentArray[i].split(Constants.SPACE_SYMBOL);
                for (var j = 0; j < subText.length; j++) {
                    if (subText[j].toLowerCase().contains("o")) {
                        subText[j] = Constants.START_TAG + subText[j] + Constants.END_TAG;
                    }
                }
                fileContentArray[i] = String.join(Constants.SPACE_SYMBOL, subText);
            }

            String formattedFileContent = String.join(Constants.SPACE_SYMBOL, fileContentArray);
            if (isContainsChinese) {
                chineseParagraphList.add(formattedFileContent);
            } else {
                englishParagraphList.add(formattedFileContent);
            }
        });

        generateOutputFile("english", englishParagraphList);
        generateOutputFile("chinese", chineseParagraphList);

        removeSourceAndDestinationFiles(imageFileNameList);
        removeSingeFileOrFolder(folderPath);
    }

    private static boolean isContainsChineseCharacter(String text) {
        for (char c : text.toCharArray()) {
            Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
            if (block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                    || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                    || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                    || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_C
                    || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_D
                    || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                    || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS_SUPPLEMENT) {
                return true;
            }
        }
        return false;
    }

    private static void generateOutputFile(String type, List<String> fileContent) {
        String fileNameOutput = type + Constants.TXT_FORMAT;
        try {
            FileWriter fileWriter = new FileWriter(fileNameOutput);
            fileWriter.write(String.join(Constants.NEWLINE_SYMBOL, fileContent));
            fileWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void removeSourceAndDestinationFiles(List<String> imageFileNameList) {
        imageFileNameList.parallelStream().forEach(imageFileName -> {
            removeSingeFileOrFolder(imageFileName);
            removeSingeFileOrFolder(Constants.IMAGE_FOLDER_PATH + "/" + imageFileName);
        });
    }

    private static void removeSingeFileOrFolder(String folderPath) {
        try {
            Files.delete(Paths.get(folderPath));
        } catch (IOException e) {
            System.err.println("Failed to copy file: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private static void createNewFolderAndCopyFile(String folderPath,
                                                   List<String> imageFileNameList) {
        java.io.File imageFolder = new java.io.File(folderPath);
        imageFolder.mkdirs();
        imageFileNameList.parallelStream().forEach(imageFileName -> {
            Path source = Paths.get(imageFileName);
            Path destination = Paths.get(Constants.IMAGE_FOLDER_PATH + "/" + imageFileName);

            try {
                Files.copy(source, destination);
            } catch (IOException e) {
                System.err.println("Failed to copy file: " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }
}