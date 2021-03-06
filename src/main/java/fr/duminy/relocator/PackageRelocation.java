package fr.duminy.relocator;

@SuppressWarnings("WeakerAccess")
public class PackageRelocation implements Relocation {
    private final String sourcePackage;
    private final String targetPackage;

    public PackageRelocation(String sourcePackage, String targetPackage) {
        this.sourcePackage = sourcePackage;
        this.targetPackage = targetPackage;
    }

    @Override
    public String getSourcePackage() {
        return sourcePackage;
    }

    @Override
    public String getTargetPackage() {
        return targetPackage;
    }
}
