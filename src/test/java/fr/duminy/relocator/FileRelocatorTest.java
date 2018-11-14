package fr.duminy.relocator;

import com.github.javaparser.ast.CompilationUnit;
import org.junit.jupiter.api.Test;

import static com.github.javaparser.JavaParser.parse;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static org.assertj.core.api.Assertions.assertThat;

class FileRelocatorTest {
    private final FileRelocator fileRelocator = new FileRelocator();

    @Test
    void relocate_package() {
        fileRelocator.addRelocation(new PackageRelocation("package1", "package2"));
        relocate("package package1;\n\n"
                 + "public class Class1 {\n"
                 + "}\n",
                 "package package2;\n\n"
                 + "public class Class1 {\n"
                 + "}\n");
    }

    @Test
    void relocate_package_longerName() {
        fileRelocator.addRelocation(new PackageRelocation("package1", "package2"));
        relocate("package package1A;\n\n"
                 + "public class Class1 {\n"
                 + "}\n",
                 "package package1A;\n\n"
                 + "public class Class1 {\n"
                 + "}\n");
    }

    @Test
    void relocate_import() {
        fileRelocator.addRelocation(new PackageRelocation("package1", "package2"));
        relocate("package userpackage;\n\n"
                 + "import package1.Class1;\n\n"
                 + "public class Class1User {\n\n"
                 + "    private Class1 class1;\n"
                 + "}\n",
                 "package userpackage;\n\n"
                 + "import package2.Class1;\n\n"
                 + "public class Class1User {\n\n"
                 + "    private Class1 class1;\n"
                 + "}\n");
    }

    @Test
    void relocate_import_longerName() {
        fileRelocator.addRelocation(new PackageRelocation("package1", "package2"));
        relocate("package userpackage;\n\n"
                 + "import package1A.Class1;\n\n"
                 + "public class Class1User {\n\n"
                 + "    private Class1 class1;\n"
                 + "}\n",
                 "package userpackage;\n\n"
                 + "import package1A.Class1;\n\n"
                 + "public class Class1User {\n\n"
                 + "    private Class1 class1;\n"
                 + "}\n");
    }

    @Test
    void relocate_class_reference() {
        fileRelocator.addRelocation(new PackageRelocation("package1", "package2"));
        relocate("package userpackage;\n\n"
                 + "public class Class1User {\n\n"
                 + "    private package1.Class1 class1 = new package1.Class1();\n"
                 + "}\n",
                 "package userpackage;\n\n"
                 + "public class Class1User {\n\n"
                 + "    private package2.Class1 class1 = new package2.Class1();\n"
                 + "}\n");
    }

    @Test
    void relocate_class_reference_longerName() {
        fileRelocator.addRelocation(new PackageRelocation("package1", "package2"));
        relocate("package userpackage;\n\n"
                 + "public class Class1User {\n\n"
                 + "    private package1A.Class1 class1 = new package1A.Class1();\n"
                 + "}\n",
                 "package userpackage;\n\n"
                 + "public class Class1User {\n\n"
                 + "    private package1A.Class1 class1 = new package1A.Class1();\n"
                 + "}\n");
    }

    @Test
    void relocate_static_import() {
        fileRelocator.addRelocation(new PackageRelocation("package1", "package2"));
        relocate("package userpackage;\n\n"
                 + "import static package1.Class1.staticMethod;\n\n"
                 + "public class Class1User {\n\n"
                 + "    public void user() {"
                 + "        staticMethod();\n"
                 + "    }\n"
                 + "}\n",
                 "package userpackage;\n\n"
                 + "import static package2.Class1.staticMethod;\n\n"
                 + "public class Class1User {\n\n"
                 + "    public void user() {\n"
                 + "        staticMethod();\n"
                 + "    }\n"
                 + "}\n");
    }

    @Test
    void relocate_static_import_longerName() {
        fileRelocator.addRelocation(new PackageRelocation("package1", "package2"));
        relocate("package userpackage;\n\n"
                 + "import static package1A.Class1.staticMethod;\n\n"
                 + "public class Class1User {\n\n"
                 + "    public void user() {"
                 + "        staticMethod();\n"
                 + "    }\n"
                 + "}\n",
                 "package userpackage;\n\n"
                 + "import static package1A.Class1.staticMethod;\n\n"
                 + "public class Class1User {\n\n"
                 + "    public void user() {\n"
                 + "        staticMethod();\n"
                 + "    }\n"
                 + "}\n");
    }

    @Test
    void relocate_static_method_reference() {
        fileRelocator.addRelocation(new PackageRelocation("package1", "package2"));
        relocate("package userpackage;\n\n"
                 + "public class Class1User {\n\n"
                 + "    public void user() {"
                 + "        package1.Class1.staticMethod();\n"
                 + "    }\n"
                 + "}\n",
                 "package userpackage;\n\n"
                 + "public class Class1User {\n\n"
                 + "    public void user() {\n"
                 + "        package2.Class1.staticMethod();\n"
                 + "    }\n"
                 + "}\n");
    }

    @Test
    void relocate_static_method_reference_longerName() {
        fileRelocator.addRelocation(new PackageRelocation("package1", "package2"));
        relocate("package userpackage;\n\n"
                 + "public class Class1User {\n\n"
                 + "    public void user() {"
                 + "        package1A.Class1.staticMethod();\n"
                 + "    }\n"
                 + "}\n",
                 "package userpackage;\n\n"
                 + "public class Class1User {\n\n"
                 + "    public void user() {\n"
                 + "        package1A.Class1.staticMethod();\n"
                 + "    }\n"
                 + "}\n");
    }

    private void relocate(String source, String expectedResult) {
        CompilationUnit compilationUnit = parse(source);
        fileRelocator.relocate(compilationUnit);
        assertThat(normalize(compilationUnit.toString())).isEqualTo(normalize(expectedResult));
    }

    private String normalize(String source) {
        return stream(source.split("\n"))
            .map(line -> line.replaceAll("\n", ""))
            .filter(line -> !line.isEmpty())
            .collect(joining("\n"));
    }
}