package net.sxlver.databases.util;

import java.io.File;
import java.net.URISyntaxException;

public class FileSystemUtil {
    public static String getJarFileDirectory(final Class<?> type) {
        try {
            return new File(type.getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile().getPath();
        }catch (final URISyntaxException exception) {
            exception.printStackTrace();
            return "";
        }
    }

    public static File[] getFilesInDirectoryNonNull(final File file) {
        final File[] files = file.listFiles();
        if(file.listFiles() == null) {
            return new File[0];
        }
        return files;
    }
}
