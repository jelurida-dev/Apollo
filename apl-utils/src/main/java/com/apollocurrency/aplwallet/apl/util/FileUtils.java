/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util;

import static org.slf4j.LoggerFactory.getLogger;

import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.Consumer;
import javax.enterprise.inject.Vetoed;

@Vetoed
public class FileUtils {
    private static final Logger log = getLogger(FileUtils.class);

    public static boolean deleteFileIfExistsQuietly(Path file) {
        try {
            return Files.deleteIfExists(file);
        }
        catch (IOException e) {
            log.error("Unable to delete file " + file, e);
        }
        return false;
    }

    public static boolean deleteFileIfExistsAndHandleException(Path file, Consumer<IOException> handler) {
        try {
            return Files.deleteIfExists(file);
        }
        catch (IOException e) {
            handler.accept(e);
        }
        return false;
    }

    public static void clearDirectorySilently(Path directory) {
        if (!Files.isDirectory(directory) || !Files.exists(directory)) {
            return;
        }
        try {
            Files.walkFileTree(directory, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                        @Override
                        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                            if (!dir.equals(directory)) {
                                Files.delete(dir);
                            }
                            return super.postVisitDirectory(dir, exc);
                        }
                    }

            );
        } catch (IOException e) {
            log.error("Unable to delete dir {}", directory);
        }
    }

    public static void deleteFilesByPattern(Path directory, String[] suffixes, String[] names) {
        if (!Files.isDirectory(directory) || !Files.exists(directory)) {
            return;
        }
        try {
            Files.list(directory).filter(p -> {
                boolean match = names == null;
                if (!match) {
                    for (String name : names) {
                        if (p.toAbsolutePath().toString().contains(name)) {
                            match = true;
                            break;
                        }
                    }
                }
                if (!match) {
                    return false;
                }
                match = suffixes == null;
                if (!match) {


                    for (String suffix : suffixes) {
                        if (p.toAbsolutePath().toString().endsWith(suffix)) {
                            return true;
                        }
                    }
                } else {
                    return true;
                }
                return false;
            }).forEach(FileUtils::deleteFileIfExistsQuietly);
        } catch (IOException e) {
            log.error("Unable to delete dir {}", directory);
        }
    }



    private FileUtils() {}
}
