package fr.duminy.relocator;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.PackageDeclaration;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

import static com.github.javaparser.JavaParser.parse;
import static java.nio.file.Files.*;
import static java.nio.file.Paths.get;
import static java.nio.file.StandardOpenOption.CREATE;

@SuppressWarnings({ "unused", "WeakerAccess" })
public class Relocator {
    private final Path sourceDirectory;
    private final FileRelocator fileRelocator;
    private final FileCollector fileCollector;

    public Relocator(Path sourceDirectory) {
        this(sourceDirectory, new FileRelocator(), new FileCollector());
    }

    Relocator(Path sourceDirectory, FileRelocator fileRelocator, FileCollector fileCollector) {
        this.sourceDirectory = sourceDirectory;
        this.fileRelocator = fileRelocator;
        this.fileCollector = fileCollector;
    }

    public void addRelocation(Relocation relocation) {
        fileRelocator.addRelocation(relocation);
    }

    public void relocate() throws IOException {
        fileCollector.collectFiles(sourceDirectory).forEach(file -> {
            try {
                String fileName = file.getFileName().toString();
                CompilationUnit compilationUnit = parse(file);
                if (relocate(compilationUnit, fileName)) {
                    generateFile(compilationUnit, fileName, file);
                }
            } catch (IOException e) {
                throw new RelocatorException(e);
            }
        });
    }

    private boolean relocate(CompilationUnit compilationUnit, String fileName) {
        System.out.println("Relocating " + fileName);
        return fileRelocator.relocate(compilationUnit);
    }

    private void generateFile(CompilationUnit compilationUnit, String fileName, Path file)
        throws IOException {
        Optional<PackageDeclaration> packageDeclaration = compilationUnit.getPackageDeclaration();
        if (!packageDeclaration.isPresent()) {
            return; //TODO handle case of default package
        }
        String packageName = packageDeclaration.get().getNameAsString().replace(".", "/");

        Path packageDirectory = sourceDirectory.resolve(get(packageName));
        createDirectories(packageDirectory);
        Path output = packageDirectory.resolve(fileName);
        if (!Objects.equals(file.toAbsolutePath().toString(), output.toAbsolutePath().toString())) {
            move(file, output);
        }
        try (Writer writer = newBufferedWriter(output, CREATE)) {
            writer.write(compilationUnit.toString());
        }
    }
}
