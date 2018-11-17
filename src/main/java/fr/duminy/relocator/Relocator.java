package fr.duminy.relocator;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.PackageDeclaration;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.Optional;

import static com.github.javaparser.JavaParser.parse;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.newBufferedWriter;
import static java.nio.file.Paths.get;
import static java.nio.file.StandardOpenOption.CREATE;

@SuppressWarnings("unused")
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

    public void relocate(Path targetDirectory) throws IOException {
        createDirectories(targetDirectory);
        fileCollector.collectFiles(sourceDirectory).forEach(file -> {
            try {
                String fileName = file.getFileName().toString();
                CompilationUnit compilationUnit = parse(file);
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
        createDirectories(parentOutput);
        Path output = parentOutput.resolve(fileName);
        try (Writer writer = newBufferedWriter(output, CREATE)) {
            writer.write(compilationUnit.toString());
        }
    }
}
