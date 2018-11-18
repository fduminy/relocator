package fr.duminy.relocator;

import com.github.javaparser.ast.CompilationUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

import static java.nio.file.Files.*;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RelocatorTest {
    @Mock private Path sourceDirectory;
    @Mock private FileRelocator fileRelocator;
    @Mock private FileCollector fileCollector;
    @Mock private Relocation relocation;

    @Test
    void addRelocation() {
        Relocator relocator = new Relocator(sourceDirectory, fileRelocator, fileCollector);

        relocator.addRelocation(relocation);

        verify(fileRelocator).addRelocation(relocation);
        verifyNoMoreInteractions(sourceDirectory, fileRelocator, fileCollector, relocation);
    }

    @Test
    void relocate() throws IOException {
        Path sourceDirectory = createTempDirectory("");
        Path file1 = createClassFile(sourceDirectory, "Class1");
        Path file2 = createClassFile(sourceDirectory, "Class2");
        when(fileCollector.collectFiles(sourceDirectory)).thenReturn(asList(file1, file2));
        Relocator relocator = new Relocator(sourceDirectory, fileRelocator, fileCollector);
        relocator.addRelocation(relocation);

        relocator.relocate();

        verify(fileRelocator).addRelocation(relocation);
        verify(fileRelocator).relocate(argThat(eqCompilationUnitFor(file1)));
        verify(fileRelocator).relocate(argThat(eqCompilationUnitFor(file2)));
        verifyNoMoreInteractions(fileRelocator, fileCollector, relocation);
        Path targetPackage = sourceDirectory.resolve("package1");
        assertThat(targetPackage.resolve(file1.getFileName().toString())).hasSameContentAs(file1);
        assertThat(targetPackage.resolve(file2.getFileName().toString())).hasSameContentAs(file2);
    }

    private Path createClassFile(Path sourceDirectory, String className) throws IOException {
        Path packageDirectory = sourceDirectory.resolve("package1");
        createDirectories(packageDirectory);
        Path file = createFile(packageDirectory.resolve(className + ".java"));
        write(file, ("package package1;\n\npublic class " + className + " {\n}").getBytes());
        return file;
    }

    private ArgumentMatcher<CompilationUnit> eqCompilationUnitFor(Path file) {
        return new ArgumentMatcher<CompilationUnit>() {
            private final String fileName = file.getFileName().toString();

            @Override public boolean matches(CompilationUnit compilationUnit) {
                String expectedFileName = compilationUnit.getPrimaryTypeName().map(className -> className + ".java")
                                                         .orElse(null);
                return Objects.equals(expectedFileName, fileName);
            }

            @Override public String toString() {
                return fileName;
            }
        };
    }
}