package fr.duminy.relocator;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.PackageDeclaration;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static com.github.javaparser.JavaParser.parse;
import static fr.duminy.relocator.FileCollector.collectFiles;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.newBufferedWriter;
import static java.nio.file.Paths.get;
import static java.nio.file.StandardOpenOption.CREATE;

public class Relocator {
    private final Path sourceDirectory;
    private final FileRelocator fileRelocator = new FileRelocator();

    public Relocator(Path sourceDirectory) {
        this.sourceDirectory = sourceDirectory;
    }

    public void addRelocation(PackageRelocation packageRelocation) {
        fileRelocator.addRelocation(packageRelocation);
    }

    public void relocate(Path targetDirectory) throws IOException {
        createDirectories(targetDirectory);
        collectFiles(sourceDirectory).forEach(file -> {
            try (InputStream in = Files.newInputStream(file)) {
                String fileName = file.getFileName().toString();
                CompilationUnit compilationUnit = parse(in);
                relocate(compilationUnit, fileName);
                generateFile(compilationUnit, targetDirectory, fileName);
            } catch (IOException e) {
                throw new RelocatorException(e);
            }
        });
    }

    private void relocate(CompilationUnit compilationUnit, String fileName) {
        System.out.println("Relocating " + fileName);
        fileRelocator.relocate(compilationUnit);
    }

    private void generateFile(CompilationUnit compilationUnit, Path targetDirectory, String fileName)
        throws IOException {
        Optional<PackageDeclaration> packageDeclaration = compilationUnit.getPackageDeclaration();
        if (!packageDeclaration.isPresent()) {
            return; //TODO handle case of default package
        }
        String packageName = packageDeclaration.get().getNameAsString().replace(".", "/");

        Path parentOutput = targetDirectory.resolve(get(packageName));
        Files.createDirectories(parentOutput);
        Path output = parentOutput.resolve(fileName);
        try (Writer writer = newBufferedWriter(output, CREATE)) {
            writer.write(compilationUnit.toString());
        }
    }
}
