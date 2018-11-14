package fr.duminy.relocator;

public class PackageRelocation {
    private final String sourcePackage;
    private final String targetPackage;

    public PackageRelocation(String sourcePackage, String targetPackage) {
        this.sourcePackage = sourcePackage;
        this.targetPackage = targetPackage;
    }

    String getSourcePackage() {
        return sourcePackage;
    }

    String getTargetPackage() {
        return targetPackage;
    }
}
