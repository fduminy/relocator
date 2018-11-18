package fr.duminy.relocator;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.nodeTypes.NodeWithType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.GenericVisitorAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

class FileRelocator {
    private final List<Relocation> relocations = new ArrayList<>();

    void addRelocation(Relocation relocation) {
        relocations.add(relocation);
    }

    boolean relocate(CompilationUnit compilationUnit) {
        boolean[] modified = new boolean[1];
        String classSimpleName = compilationUnit.getPrimaryTypeName().orElse("");
        compilationUnit.accept(new GenericVisitorAdapter<Object, Object>() {
            @Override public Object visit(PackageDeclaration n, Object arg) {
                replacePackage(n, classSimpleName, modified);
                return super.visit(n, arg);
            }

            @Override public Object visit(CompilationUnit n, Object arg) {
                for (ImportDeclaration importDeclaration : compilationUnit.getImports()) {
                    replacePackage(importDeclaration, classSimpleName, modified);
                }
                return super.visit(n, arg);
            }

            @Override public Object visit(VariableDeclarator n, Object arg) {
                replacePackage((NodeWithType) n, classSimpleName, modified);
                return super.visit(n, arg);
            }

            @Override public Object visit(NameExpr n, Object arg) {
                replacePackage(n, classSimpleName, modified);
                return super.visit(n, arg);
            }

            @Override public Object visit(ClassOrInterfaceType n, Object arg) {
                replacePackage(n, classSimpleName, modified);
                return super.visit(n, arg);
            }

        }, null);
        return modified[0];
    }

    private void replacePackage(NodeWithType node, String classSimpleName, boolean[] modified) {
        replacePackage(node.getType().toString(), node::setType, classSimpleName, modified);
    }

    private void replacePackage(NodeWithName node, String classSimpleName, boolean[] modified) {
        replacePackage(node.getName().toString(), node::setName, classSimpleName, modified);
    }

    private void replacePackage(NodeWithSimpleName node, String classSimpleName, boolean[] modified) {
        replacePackage(node.getName().toString(), node::setName, classSimpleName, modified);
    }

    private void replacePackage(String name, Consumer<String> nameSetter, String classSimpleName, boolean[] modified) {
        for (Relocation relocation : relocations) {
            if (doesNotRelocateClass(relocation, classSimpleName)) {
                continue;
            }

            if (name.equals(relocation.getSourcePackage())) {
                nameSetter.accept(relocation.getTargetPackage());
                modified[0] = true;
                break;
            }
            if (name.startsWith(relocation.getSourcePackage() + '.')) {
                nameSetter.accept(name.replace(relocation.getSourcePackage(), relocation.getTargetPackage()));
                modified[0] = true;
                break;
            }
        }
    }

    private boolean doesNotRelocateClass(Relocation relocation, String classSimpleName) {
        return (relocation instanceof ClassRelocation) && !Objects
            .equals(((ClassRelocation) relocation).getSourceClass(), classSimpleName);
    }
}
