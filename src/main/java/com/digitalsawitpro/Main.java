package com.digitalsawitpro;

import com.digitalsawitpro.constants.Constants;
import com.digitalsawitpro.service.GoogleDriveService;
import com.digitalsawitpro.service.GoogleVisionService;
import com.digitalsawitpro.utils.FileUtils;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.auth.Credentials;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.ImageAnnotatorSettings;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) throws IOException, GeneralSecurityException {
        //CREATE CREDENTIAL FOR GOOGLE DRIVE ACCESS
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Credential googleDriveCredential = GoogleDriveService.getGoogleDriveCredential(
                HTTP_TRANSPORT);
        Drive service = GoogleDriveService.getDriveInstance(HTTP_TRANSPORT, googleDriveCredential);

        //FIND FOLDER ID IN GOOGLE DRIVE
        String queryToFindFolderId = "mimeType='application/vnd.google-apps.folder' and name='" +
                Constants.FOLDER_NAME_IN_GDRIVE + "' and trashed = false";
        FileList folderTargetList = GoogleDriveService.getFileListByQuery(service,
                queryToFindFolderId);
        String folderId = folderTargetList.getFiles().get(0).getId();
        if (folderId == null) {
            System.out.println("Folder not found: " + Constants.FOLDER_NAME_IN_GDRIVE);
            return;
        }

        //FIND FILE DATA IN A FOLDER
        String queryToFindFilesByFolderId = "parents='" + folderId + "'";
        FileList result = GoogleDriveService.getFileListByQuery(service,
                queryToFindFilesByFolderId);
        List<File> files = result.getFiles();

        //DOWNLOAD IMAGE FILE FROM GOOGLE DRIVE TO LOCAL
        List<String> imageFileNameList = new ArrayList<>();
        GoogleDriveService.downloadFileFromGoogleDrive(files, imageFileNameList, service);

        //CREATE CREDENTIALS FOR GOOGLE VISION
        Credentials credentials = GoogleVisionService.getGoogleVisionCredentials();
        ImageAnnotatorSettings settings = GoogleVisionService.getImageAnnotatorSettings(
                credentials);
        ImageAnnotatorClient finalClient = GoogleVisionService.getImageAnnotatorClient(settings);

        //INITIATE PARAGRAPH LIST
        List<String> englishParagraphList = new ArrayList<>();
        List<String> chineseParagraphList = new ArrayList<>();

        //GENERATE & MODIFY TEXT FROM DOWNLOADED IMAGE
        //CHECK THE TEXT CONTAINS CHINESE CHAR OR NOT
        imageFileNameList.stream().forEach(imageFileName -> {
            Path imagePath = Paths.get(imageFileName);
            String fileContent = GoogleVisionService.extractTextFromImage(imagePath,
                    finalClient);
            inputContentToParagraphList(fileContent, englishParagraphList, chineseParagraphList);
        });

        //GENERATE OUTPUT FILE AND REMOVE DOWNLOADED FILES
        FileUtils.generateOutputFile("english", englishParagraphList);
        FileUtils.generateOutputFile("chinese", chineseParagraphList);
        FileUtils.removeDownloadedFiles(imageFileNameList);
    }

    private static void inputContentToParagraphList(String fileContent,
                                                           List<String> englishParagraphList,
                                                           List<String> chineseParagraphList) {
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
}