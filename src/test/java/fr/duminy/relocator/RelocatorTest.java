package fr.duminy.relocator;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.comments.LineComment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Objects;

import static java.lang.Boolean.TRUE;
import static java.lang.Thread.sleep;
import static java.nio.file.Files.*;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RelocatorTest {
    @Mock private FileRelocator fileRelocator;
    @Mock private FileCollector fileCollector;

    @Test
    void addRelocation(@Mock Path sourceDirectory, @Mock Relocation relocation) {
        Relocator relocator = new Relocator(sourceDirectory, fileRelocator, fileCollector);

        relocator.addRelocation(relocation);

        verify(fileRelocator).addRelocation(relocation);
        verifyNoMoreInteractions(sourceDirectory, fileRelocator, fileCollector, relocation);
    }

    @Test
    void relocate_modifies_file() throws IOException, InterruptedException {
        relocate(true);
    }

    @Test
    void relocate_moves_file() throws IOException {
        Path sourceDirectory = createTempDirectory("");
        StringBuilder expectedFileContent = new StringBuilder();
        Path sourceFile = createClassFile(sourceDirectory, "Class1", expectedFileContent);
        when(fileCollector.collectFiles(sourceDirectory)).thenReturn(singletonList(sourceFile));
        when(fileRelocator.relocate(argThat(eqCompilationUnitFor(sourceFile))))
            .then(modifyCompilationUnitPackage(expectedFileContent, "package2"));
        Relocator relocator = new Relocator(sourceDirectory, fileRelocator, fileCollector);

        relocator.relocate();

        Path targetFile = sourceDirectory.resolve("package2").resolve(sourceFile.getFileName().toString());
        assertThat(sourceFile).doesNotExist();
        assertThat(targetFile).hasContent(expectedFileContent.toString());
    }

    @Test
    void relocate_does_not_modify_file() throws IOException, InterruptedException {
        relocate(false);
    }

    private void relocate(boolean modifyFile) throws IOException, InterruptedException {
        Path sourceDirectory = createTempDirectory("");
        StringBuilder expectedFileContent = new StringBuilder();
        Path file = createClassFile(sourceDirectory, "Class1", expectedFileContent);
        FileTime initialFileTime = getLastModifiedTime(file);
        sleep(1000); // wait next second
        when(fileCollector.collectFiles(sourceDirectory)).thenReturn(singletonList(file));
        if (modifyFile) {
            when(fileRelocator.relocate(argThat(eqCompilationUnitFor(file))))
                .then(modifyCompilationUnit("modification", expectedFileContent));
        }
        Relocator relocator = new Relocator(sourceDirectory, fileRelocator, fileCollector);

        relocator.relocate();

        verify(fileRelocator).relocate(argThat(eqCompilationUnitFor(file)));
        verifyNoMoreInteractions(fileRelocator, fileCollector);
        Path targetPackage = sourceDirectory.resolve("package1");
        assertThat(targetPackage.resolve(file.getFileName().toString())).hasContent(expectedFileContent.toString());
        if (modifyFile) {
            assertThat(getLastModifiedTime(file)).isGreaterThan(initialFileTime);
        } else {
            assertThat(getLastModifiedTime(file)).isEqualTo(initialFileTime);
        }
    }

    private Answer<Boolean> modifyCompilationUnitPackage(StringBuilder fileContent,
                                                         String targetPackage) {
        return invocationOnMock -> {
            CompilationUnit compilationUnit = invocationOnMock.getArgument(0);
            compilationUnit.getPackageDeclaration().ifPresent(declaration -> declaration.setName(targetPackage));
            String content = fileContent.toString();
            fileContent.setLength(0);
            fileContent.append(content.replace("package1", targetPackage));
            return TRUE;
        };
    }

    private Answer<Boolean> modifyCompilationUnit(String modification, StringBuilder fileContent) {
        return invocationOnMock -> {
            CompilationUnit compilationUnit = invocationOnMock.getArgument(0);
            compilationUnit.addOrphanComment(new LineComment(modification));
            fileContent.append("\n").append("// ").append(modification);
            return TRUE;
        };
    }

    private Path createClassFile(Path sourceDirectory, String className, StringBuilder fileContent) throws IOException {
        Path packageDirectory = sourceDirectory.resolve("package1");
        createDirectories(packageDirectory);
        Path file = createFile(packageDirectory.resolve(className + ".java"));
        String sourceCode = "package package1;\n\npublic class " + className + " {\n}";
        fileContent.append(sourceCode);
        write(file, sourceCode.getBytes());
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