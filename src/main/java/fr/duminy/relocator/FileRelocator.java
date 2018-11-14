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
import java.util.function.Consumer;

class FileRelocator {
    private final List<PackageRelocation> relocations = new ArrayList<>();

    void addRelocation(PackageRelocation packageRelocation) {
        relocations.add(packageRelocation);
    }

    void relocate(CompilationUnit compilationUnit) {
        compilationUnit.accept(new GenericVisitorAdapter<Object, Object>() {
            @Override public Object visit(PackageDeclaration n, Object arg) {
                replacePackage(n);
                return super.visit(n, arg);
            }

            @Override public Object visit(CompilationUnit n, Object arg) {
                for (ImportDeclaration importDeclaration : compilationUnit.getImports()) {
                    replacePackage(importDeclaration);
                }
                return super.visit(n, arg);
            }

            @Override public Object visit(VariableDeclarator n, Object arg) {
                replacePackage((NodeWithType) n);
                return super.visit(n, arg);
            }

            @Override public Object visit(NameExpr n, Object arg) {
                replacePackage(n);
                return super.visit(n, arg);
            }

            @Override public Object visit(ClassOrInterfaceType n, Object arg) {
                replacePackage(n);
                return super.visit(n, arg);
            }

        }, null);
    }

    private void replacePackage(NodeWithType node) {
        replacePackage(node.getType().toString(), node::setType);
    }

    private void replacePackage(NodeWithName node) {
        replacePackage(node.getName().toString(), node::setName);
    }

    private void replacePackage(NodeWithSimpleName node) {
        replacePackage(node.getName().toString(), node::setName);
    }

    private void replacePackage(String name, Consumer<String> nameSetter) {
        for (PackageRelocation packageRelocation : relocations) {
            if (name.equals(packageRelocation.getSourcePackage())) {
                nameSetter.accept(packageRelocation.getTargetPackage());
                break;
            }
            if (name.startsWith(packageRelocation.getSourcePackage() + '.')) {
                nameSetter
                    .accept(name.replace(packageRelocation.getSourcePackage(), packageRelocation.getTargetPackage()));
                break;
            }
        }
    }
}
