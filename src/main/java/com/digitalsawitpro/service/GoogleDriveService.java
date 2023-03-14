package com.digitalsawitpro.service;

import com.digitalsawitpro.Main;
import com.digitalsawitpro.constants.Constants;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.*;
import java.util.List;

public class GoogleDriveService {

    public static Credential getGoogleDriveCredential(final NetHttpTransport HTTP_TRANSPORT)
            throws IOException {
        InputStream inputStream = Main.class.getResourceAsStream(Constants.GOOGLE_OAUTH_PATH);
        if (inputStream == null) {
            throw new FileNotFoundException("Credential file not found: " +
                    Constants.GOOGLE_OAUTH_PATH);
        }
        GoogleClientSecrets googleClientSecrets = GoogleClientSecrets.load(
                Constants.JSON_FACTORY, new InputStreamReader(inputStream));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, Constants.JSON_FACTORY, googleClientSecrets, Constants.SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File("tokens")))
                .setAccessType("offline")
                .build();

        LocalServerReceiver receiver = new LocalServerReceiver.Builder()
                .setPort(8080).build();
        Credential credential = new AuthorizationCodeInstalledApp(flow, receiver)
                .authorize("user");

        return credential;
    }

    public static Drive getDriveInstance(NetHttpTransport HTTP_TRANSPORT,
                                         Credential credential) {
        return new Drive.Builder(HTTP_TRANSPORT, Constants.JSON_FACTORY,
                credential)
                .setApplicationName(Constants.APPLICATION_NAME)
                .build();
    }

    public static FileList getFileListByQuery(Drive service, String Query)
            throws IOException {
        return service.files().list()
                .setQ(Query)
                .setFields("nextPageToken, files(id, name)")
                .execute();
    }

    public static void downloadFileFromGoogleDrive(List<File> files,
                                                   List<String> imageFileNameList,
                                                   Drive service) throws IOException {
        if (files == null || files.isEmpty()) {
            throw new FileNotFoundException();
        } else {
            for (File file : files) {
                imageFileNameList.add(file.getName());
                OutputStream outputStream = new FileOutputStream(file.getName());
                service.files().get(file.getId()).executeMediaAndDownloadTo(outputStream);
            }
        }
    }
}
