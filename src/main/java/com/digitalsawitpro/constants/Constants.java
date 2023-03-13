package com.digitalsawitpro.constants;

import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.DriveScopes;

import java.util.Collections;
import java.util.List;

public class Constants {
    public static final String APPLICATION_NAME = "Google API";
    public static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    public static final String CREDENTIALS_FILE_PATH = "northern-saga-380405-2bc8cd3fcf47.json";
    public static final String GOOGLE_OAUTH_PATH = "/credentials.json";
    public static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE_READONLY);
    public static final String FOLDER_NAME_IN_GDRIVE = "Digital SawitPRO - Test";
    public static final String IMAGE_FOLDER_PATH = "src/main/resources/image";
    public static final String NEWLINE_SYMBOL = "\\n";
    public static final String SPACE_SYMBOL = " ";
    public static final String TXT_FORMAT = ".txt";
    public static final String START_TAG = "<span style=\"color:blue\">";
    public static final String END_TAG = "</span>";
}
