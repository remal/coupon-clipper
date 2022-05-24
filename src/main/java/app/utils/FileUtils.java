package app.utils;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.Files.deleteIfExists;
import static java.nio.file.Files.walkFileTree;
import static lombok.AccessLevel.PRIVATE;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

@NoArgsConstructor(access = PRIVATE)
public abstract class FileUtils {

    @SneakyThrows
    public static void deleteFile(Path path) {
        try {
            walkFileTree(path.toAbsolutePath(), new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    deleteIfExists(dir);
                    return CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    deleteIfExists(file);
                    return CONTINUE;
                }
            });
        } catch (NoSuchFileException ignored) {
            // do nothing
        }
    }

    public static void deleteFile(File file) {
        deleteFile(file.toPath());
    }

}
