package fr.duminy.relocator;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.Files.walkFileTree;

@SuppressWarnings("WeakerAccess")
public class FileCollector {
    public List<Path> collectFiles(Path directory) throws IOException {
        List<Path> files = new ArrayList<>();
        walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
                if (attr.isRegularFile() && file.getFileName().toString().endsWith(".java")) {
                    files.add(file.toAbsolutePath());
                }
                return CONTINUE;
            }
        });
        return files;
    }
}
