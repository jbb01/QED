package com.jonahbauer.qed.util;

import androidx.annotation.NonNull;
import lombok.experimental.UtilityClass;

import java.io.File;
import java.io.IOException;

@UtilityClass
public class FileUtils {

    /**
     * Cleans the given directory by recursively deleting all files in it leaving the directory itself untouched.
     * An error deleting one file will not affect deletion of other files.
     *
     * @throws IOException when an I/O error occurred or at least one file could not be deleted
     * @throws IllegalArgumentException when the given directory does not exist or is not a directory.
     */
    public static void cleanDirectory(@NonNull File directory) throws IOException {
        if (!directory.isDirectory()) throw new IllegalArgumentException("Not a directory: " + directory + ".");
        if (!directory.exists()) throw new IllegalArgumentException("Directory does not exits: " + directory + ".");

        var files = directory.listFiles();
        if (files == null) throw new IOException("Could not list files in directory: " + directory + ".");

        var exception = (IOException) null;
        for (var file : files) {
            try {
                if (file.isDirectory()) {
                    cleanDirectory(file);
                }
                if (!file.delete()) {
                    throw new IOException("Error deleting file: " + file);
                }
            } catch (IOException e) {
                if (exception == null) exception = new IOException("Could not clean directory: " + directory + ".");
                exception.addSuppressed(e);
            }
        }
        if (exception != null) throw exception;
    }
}
