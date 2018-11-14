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

public class FileCollector extends SimpleFileVisitor<Path> {
    private final List<Path> files = new ArrayList<>();

    public static List<Path> collectFiles(Path output) throws IOException {
        FileCollector fileCollector = new FileCollector();
        walkFileTree(output, fileCollector);
        return fileCollector.getFiles();
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
        if (attr.isRegularFile() && file.getFileName().toString().endsWith(".java")) {
            files.add(file.toAbsolutePath());
        }
        return CONTINUE;
    }

    public List<Path> getFiles() {
        return files;
    }
}
