package com.digitalsawitpro.utils;

import com.digitalsawitpro.constants.Constants;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class FileUtils {

    public static void generateOutputFile(String type, List<String> fileContent) {
        String fileNameOutput = type + Constants.TXT_FORMAT;
        try {
            FileWriter fileWriter = new FileWriter(fileNameOutput);
            fileWriter.write(String.join(Constants.NEWLINE_SYMBOL, fileContent));
            fileWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void removeDownloadedFiles(List<String> imageFileNameList) {
        imageFileNameList.parallelStream()
                .forEach(imageFileName -> removeSingeFileOrFolder(imageFileName));
    }

    private static void removeSingeFileOrFolder(String folderPath) {
        try {
            Files.delete(Paths.get(folderPath));
        } catch (IOException e) {
            System.err.println("Failed to copy file: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
