package fr.duminy.relocator;

import com.github.javaparser.ast.CompilationUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Function;

import static com.github.javaparser.JavaParser.parse;
import static java.nio.file.Files.createTempDirectory;
import static java.nio.file.Files.write;
import static java.util.Arrays.stream;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.joining;
import static org.assertj.core.api.Assertions.assertThat;

class FileRelocatorTest {
    private static final Function<String, String> PACKAGE1_TO_PACKAGE2 = source -> source
        .replace("package1", "package2");
    private static final PackageRelocation PACKAGE1_TO_PACKAGE2_RELOCATION = new PackageRelocation("package1",
                                                                                                   "package2");
    private static final ClassRelocation CLASS1_TO_PACKAGE2_RELOCATION = new ClassRelocation("package1", "Class1",
                                                                                             "package2");

    private final FileRelocator fileRelocator = new FileRelocator();

    @Test
    void relocate_package_class2() throws Exception {
        fileRelocator.addRelocation(CLASS1_TO_PACKAGE2_RELOCATION);
        relocate("package package1;\n\n"
                 + "public class Class2 {\n"
                 + "}\n", identity());
    }

    @ParameterizedTest
    @ValueSource(strings = { "true", "false" })
    void relocate_package(boolean packageRelocation) throws Exception {
        fileRelocator
            .addRelocation(packageRelocation ? PACKAGE1_TO_PACKAGE2_RELOCATION : CLASS1_TO_PACKAGE2_RELOCATION);
        relocate("package package1;\n\n"
                 + "public class Class1 {\n"
                 + "}\n", PACKAGE1_TO_PACKAGE2);
    }

    @ParameterizedTest
    @ValueSource(strings = { "true", "false" })
    void relocate_package_longerName(boolean packageRelocation) throws Exception {
        fileRelocator
            .addRelocation(packageRelocation ? PACKAGE1_TO_PACKAGE2_RELOCATION : CLASS1_TO_PACKAGE2_RELOCATION);
        relocate("package package1A;\n\n"
                 + "public class Class1 {\n"
                 + "}\n", identity());
    }

    @Test
    void relocate_import_class2() throws Exception {
        fileRelocator.addRelocation(CLASS1_TO_PACKAGE2_RELOCATION);
        relocate("package userpackage;\n\n"
                 + "import package1.Class2;\n\n"
                 + "public class Class2User {\n\n"
                 + "    private Class12 class2;\n"
                 + "}\n", identity());
    }

    @Test
    void relocate_import() throws Exception {
        fileRelocator.addRelocation(PACKAGE1_TO_PACKAGE2_RELOCATION);
        relocate("package userpackage;\n\n"
                 + "import package1.Class1;\n\n"
                 + "public class Class1User {\n\n"
                 + "    private Class1 class1;\n"
                 + "}\n", PACKAGE1_TO_PACKAGE2);
    }

    @Test
    void relocate_import_longerName() throws Exception {
        fileRelocator.addRelocation(PACKAGE1_TO_PACKAGE2_RELOCATION);
        relocate("package userpackage;\n\n"
                 + "import package1A.Class1;\n\n"
                 + "public class Class1User {\n\n"
                 + "    private Class1 class1;\n"
                 + "}\n", identity());
    }

    @Test
    void relocate_class_reference_class2() throws Exception {
        fileRelocator.addRelocation(CLASS1_TO_PACKAGE2_RELOCATION);
        relocate("package userpackage;\n\n"
                 + "public class Class2User {\n\n"
                 + "    private package1.Class2 class1 = new package1.Class2();\n"
                 + "}\n", identity());
    }

    @Test
    void relocate_class_reference() throws Exception {
        fileRelocator.addRelocation(PACKAGE1_TO_PACKAGE2_RELOCATION);
        relocate("package userpackage;\n\n"
                 + "public class Class1User {\n\n"
                 + "    private package1.Class1 class1 = new package1.Class1();\n"
                 + "}\n", PACKAGE1_TO_PACKAGE2);
    }

    @Test
    void relocate_class_reference_longerName() throws Exception {
        fileRelocator.addRelocation(PACKAGE1_TO_PACKAGE2_RELOCATION);
        relocate("package userpackage;\n\n"
                 + "public class Class1User {\n\n"
                 + "    private package1A.Class1 class1 = new package1A.Class1();\n"
                 + "}\n", identity());
    }

    @Test
    void relocate_static_import_class2() throws Exception {
        fileRelocator.addRelocation(CLASS1_TO_PACKAGE2_RELOCATION);
        relocate("package userpackage;\n\n"
                 + "import static package1.Class2.staticMethod;\n\n"
                 + "public class Class2User {\n\n"
                 + "    public void user() throws Exception {\n"
                 + "        staticMethod();\n"
                 + "    }\n"
                 + "}\n", identity());
    }

    @Test
    void relocate_static_import() throws Exception {
        fileRelocator.addRelocation(PACKAGE1_TO_PACKAGE2_RELOCATION);
        relocate("package userpackage;\n\n"
                 + "import static package1.Class1.staticMethod;\n\n"
                 + "public class Class1User {\n\n"
                 + "    public void user() throws Exception {\n"
                 + "        staticMethod();\n"
                 + "    }\n"
                 + "}\n", PACKAGE1_TO_PACKAGE2);
    }

    @Test
    void relocate_static_import_longerName() throws Exception {
        fileRelocator.addRelocation(PACKAGE1_TO_PACKAGE2_RELOCATION);
        relocate("package userpackage;\n\n"
                 + "import static package1A.Class1.staticMethod;\n\n"
                 + "public class Class1User {\n\n"
                 + "    public void user() throws Exception {\n"
                 + "        staticMethod();\n"
                 + "    }\n"
                 + "}\n", identity());
    }

    @Test
    void relocate_static_method_reference_class2() throws Exception {
        fileRelocator.addRelocation(CLASS1_TO_PACKAGE2_RELOCATION);
        relocate("package userpackage;\n\n"
                 + "public class Class2User {\n\n"
                 + "    public void user() {\n"
                 + "        package1.Class2.staticMethod();\n"
                 + "    }\n"
                 + "}\n", identity());
    }

    @Test
    void relocate_static_method_reference() throws Exception {
        fileRelocator.addRelocation(PACKAGE1_TO_PACKAGE2_RELOCATION);
        relocate("package userpackage;\n\n"
                 + "public class Class1User {\n\n"
                 + "    public void user() {\n"
                 + "        package1.Class1.staticMethod();\n"
                 + "    }\n"
                 + "}\n", PACKAGE1_TO_PACKAGE2);
    }

    @Test
    void relocate_static_method_reference_longerName() throws Exception {
        fileRelocator.addRelocation(PACKAGE1_TO_PACKAGE2_RELOCATION);
        relocate("package userpackage;\n\n"
                 + "public class Class1User {\n\n"
                 + "    public void user() {\n"
                 + "        package1A.Class1.staticMethod();\n"
                 + "    }\n"
                 + "}\n", identity());
    }

    private void relocate(String source, Function<String, String> expectedResult) throws IOException {
        relocate(source, expectedResult.apply(source));
    }

    private void relocate(String source, String expectedResult) throws IOException {
        CompilationUnit compilationUnit = parse(writeFile(source));
        fileRelocator.relocate(compilationUnit);
        assertThat(normalize(compilationUnit.toString()))
            .isEqualTo(normalize((expectedResult == null) ? source : expectedResult));
    }

    private Path writeFile(String source) throws IOException {
        String withoutPackageDeclaration = source.substring(source.indexOf("public"));
        String className = withoutPackageDeclaration.substring("public class ".length()).split(" ")[0];
        Path file = createTempDirectory("").resolve(className);
        write(file, source.getBytes());
        return file;
    }

    private String normalize(String source) {
        return stream(source.split("\n"))
            .map(line -> line.replaceAll("\n", ""))
            .filter(line -> !line.isEmpty())
            .collect(joining("\n"));
    }
}