package fr.duminy.relocator;

import com.github.javaparser.ast.CompilationUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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
    private static final Function<String, String> IDENTITY = identity();

    private final FileRelocator fileRelocator = new FileRelocator();

    @Nested class ClassRelocation_ {
        private final ClassRelocation CLASS1_TO_PACKAGE2 = new ClassRelocation("package1", "Class1", "package2");

        @Nested class Package {
            @DisplayName("relocates class1")
            @Test void class1() throws Exception {
                relocate(CLASS1_TO_PACKAGE2, "package package1;\n\n"
                                             + "public class Class1 {\n"
                                             + "}\n", PACKAGE1_TO_PACKAGE2);
            }

            @DisplayName("doesn't modify class2")
            @Test void class2() throws Exception {
                relocate(CLASS1_TO_PACKAGE2, "package package1;\n\n"
                                             + "public class Class2 {\n"
                                             + "}\n", IDENTITY);
            }

            @DisplayName("doesn't modify class1 from package with longer name")
            @Test void package_with_longer_name() throws Exception {
                relocate(CLASS1_TO_PACKAGE2, "package package1A;\n\n"
                                             + "public class Class1 {\n"
                                             + "}\n", IDENTITY);
            }
        }

        @Nested class Import {
            @DisplayName("doesn't modify class2 import")
            @Test void class2() throws Exception {
                relocate(CLASS1_TO_PACKAGE2, "package userpackage;\n\n"
                                             + "import package1.Class2;\n\n"
                                             + "public class Class2User {\n\n"
                                             + "    private Class2 class2;\n"
                                             + "}\n", IDENTITY);
            }
        }

        @Nested class ClassReference {
            @DisplayName("doesn't modify class2 reference")
            @Test void class2() throws Exception {
                relocate(CLASS1_TO_PACKAGE2, "package userpackage;\n\n"
                                             + "public class Class2User {\n\n"
                                             + "    private package1.Class2 class1 = new package1.Class2();\n"
                                             + "}\n", IDENTITY);
            }
        }

        @Nested class StaticImport {
            @DisplayName("doesn't modify static import from class2")
            @Test void class2() throws Exception {
                relocate(CLASS1_TO_PACKAGE2, "package userpackage;\n\n"
                                             + "import static package1.Class2.staticMethod;\n\n"
                                             + "public class Class2User {\n\n"
                                             + "    public void user() throws Exception {\n"
                                             + "        staticMethod();\n"
                                             + "    }\n"
                                             + "}\n", IDENTITY);
            }
        }

        @Nested class StaticMethodReference {
            @DisplayName("doesn't modify static method reference to class2")
            @Test void class2() throws Exception {
                relocate(CLASS1_TO_PACKAGE2, "package userpackage;\n\n"
                                             + "public class Class2User {\n\n"
                                             + "    public void user() {\n"
                                             + "        package1.Class2.staticMethod();\n"
                                             + "    }\n"
                                             + "}\n", IDENTITY);
            }
        }
    }

    @Nested class PackageRelocation_ {
        private final PackageRelocation PACKAGE1_TO_PACKAGE2_ = new PackageRelocation("package1", "package2");

        @Nested class Package {
            @DisplayName("relocates package1")
            @Test void package1() throws Exception {
                relocate(PACKAGE1_TO_PACKAGE2_, "package package1;\n\n"
                                                + "public class Class1 {\n"
                                                + "}\n", PACKAGE1_TO_PACKAGE2);
            }

            @DisplayName("doesn't modify package with longer name")
            @Test void package_with_longer_name() throws Exception {
                relocate(PACKAGE1_TO_PACKAGE2_, "package package1A;\n\n"
                                                + "public class Class1 {\n"
                                                + "}\n", IDENTITY);
            }
        }

        @Nested class Import {
            @DisplayName("modify class1 import")
            @Test void class1() throws Exception {
                relocate(PACKAGE1_TO_PACKAGE2_, "package userpackage;\n\n"
                                                + "import package1.Class1;\n\n"
                                                + "public class Class1User {\n\n"
                                                + "    private Class1 class1;\n"
                                                + "}\n", PACKAGE1_TO_PACKAGE2);
            }

            @DisplayName("doesn't modify class1 import from package with longer name")
            @Test void package_with_longer_name() throws Exception {
                relocate(PACKAGE1_TO_PACKAGE2_, "package userpackage;\n\n"
                                                + "import package1A.Class1;\n\n"
                                                + "public class Class1User {\n\n"
                                                + "    private Class1 class1;\n"
                                                + "}\n", IDENTITY);
            }
        }

        @Nested class ClassReference {
            @DisplayName("modify class1 reference")
            @Test void class1() throws Exception {
                relocate(PACKAGE1_TO_PACKAGE2_, "package userpackage;\n\n"
                                                + "public class Class1User {\n\n"
                                                + "    private package1.Class1 class1 = new package1.Class1();\n"
                                                + "}\n", PACKAGE1_TO_PACKAGE2);
            }

            @DisplayName("doesn't modify class1 reference from package with longer name")
            @Test void package_with_longer_name() throws Exception {
                relocate(PACKAGE1_TO_PACKAGE2_, "package userpackage;\n\n"
                                                + "public class Class1User {\n\n"
                                                + "    private package1A.Class1 class1 = new package1A.Class1();\n"
                                                + "}\n", IDENTITY);
            }
        }

        @Nested class StaticImport {
            @DisplayName("modify class1 reference")
            @Test void class1() throws Exception {
                relocate(PACKAGE1_TO_PACKAGE2_, "package userpackage;\n\n"
                                                + "import static package1.Class1.staticMethod;\n\n"
                                                + "public class Class1User {\n\n"
                                                + "    public void user() throws Exception {\n"
                                                + "        staticMethod();\n"
                                                + "    }\n"
                                                + "}\n", PACKAGE1_TO_PACKAGE2);
            }

            @DisplayName("doesn't modify static import from class1 and package with longer name")
            @Test void package_with_longer_name() throws Exception {
                relocate(PACKAGE1_TO_PACKAGE2_, "package userpackage;\n\n"
                                                + "import static package1A.Class1.staticMethod;\n\n"
                                                + "public class Class1User {\n\n"
                                                + "    public void user() throws Exception {\n"
                                                + "        staticMethod();\n"
                                                + "    }\n"
                                                + "}\n", IDENTITY);
            }
        }

        @Nested class StaticMethodReference {
            @DisplayName("modify static method reference to class1")
            @Test void class1() throws Exception {
                relocate(PACKAGE1_TO_PACKAGE2_, "package userpackage;\n\n"
                                                + "public class Class1User {\n\n"
                                                + "    public void user() {\n"
                                                + "        package1.Class1.staticMethod();\n"
                                                + "    }\n"
                                                + "}\n", PACKAGE1_TO_PACKAGE2);
            }

            @DisplayName("doesn't modify static method reference from package with longer name")
            @Test void package_with_longer_name() throws Exception {
                relocate(PACKAGE1_TO_PACKAGE2_, "package userpackage;\n\n"
                                                + "public class Class1User {\n\n"
                                                + "    public void user() {\n"
                                                + "        package1A.Class1.staticMethod();\n"
                                                + "    }\n"
                                                + "}\n", IDENTITY);
            }
        }
    }

    private void relocate(Relocation relocation, String source, Function<String, String> expectedResultFunction)
        throws IOException {
        fileRelocator.addRelocation(relocation);
        CompilationUnit compilationUnit = parse(writeFile(source));

        boolean modified = fileRelocator.relocate(compilationUnit);

        assertThat(normalize(compilationUnit.toString())).isEqualTo(normalize(expectedResultFunction.apply(source)));
        assertThat(modified).isEqualTo(expectedResultFunction != IDENTITY);
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