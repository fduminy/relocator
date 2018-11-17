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

    void relocate(CompilationUnit compilationUnit) {
        String classSimpleName = compilationUnit.getPrimaryTypeName().orElse("");
        compilationUnit.accept(new GenericVisitorAdapter<Object, Object>() {
            @Override public Object visit(PackageDeclaration n, Object arg) {
                replacePackage(n, classSimpleName);
                return super.visit(n, arg);
            }

            @Override public Object visit(CompilationUnit n, Object arg) {
                for (ImportDeclaration importDeclaration : compilationUnit.getImports()) {
                    replacePackage(importDeclaration, classSimpleName);
                }
                return super.visit(n, arg);
            }

            @Override public Object visit(VariableDeclarator n, Object arg) {
                replacePackage((NodeWithType) n, classSimpleName);
                return super.visit(n, arg);
            }

            @Override public Object visit(NameExpr n, Object arg) {
                replacePackage(n, classSimpleName);
                return super.visit(n, arg);
            }

            @Override public Object visit(ClassOrInterfaceType n, Object arg) {
                replacePackage(n, classSimpleName);
                return super.visit(n, arg);
            }

        }, null);
    }

    private void replacePackage(NodeWithType node, String classSimpleName) {
        replacePackage(node.getType().toString(), node::setType, classSimpleName);
    }

    private void replacePackage(NodeWithName node, String classSimpleName) {
        replacePackage(node.getName().toString(), node::setName, classSimpleName);
    }

    private void replacePackage(NodeWithSimpleName node, String classSimpleName) {
        replacePackage(node.getName().toString(), node::setName, classSimpleName);
    }

    private void replacePackage(String name, Consumer<String> nameSetter, String classSimpleName) {
        for (Relocation relocation : relocations) {
            if (doesNotRelocateClass(relocation, classSimpleName)) {
                continue;
            }

            if (name.equals(relocation.getSourcePackage())) {
                nameSetter.accept(relocation.getTargetPackage());
                break;
            }
            if (name.startsWith(relocation.getSourcePackage() + '.')) {
                nameSetter
                    .accept(name.replace(relocation.getSourcePackage(), relocation.getTargetPackage()));
                break;
            }
        }
    }

    private boolean doesNotRelocateClass(Relocation relocation, String classSimpleName) {
        return (relocation instanceof ClassRelocation) && !Objects
            .equals(((ClassRelocation) relocation).getSourceClass(), classSimpleName);
    }
}
