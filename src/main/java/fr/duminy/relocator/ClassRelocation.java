package fr.duminy.relocator;

@SuppressWarnings("WeakerAccess")
public class ClassRelocation implements Relocation {
    private final String sourcePackage;
    private final String sourceClass;
    private final String targetPackage;

    public ClassRelocation(String sourcePackage, String sourceClass, String targetPackage) {
        this.sourcePackage = sourcePackage;
        this.sourceClass = sourceClass;
        this.targetPackage = targetPackage;
    }

    @Override
    public String getSourcePackage() {
        return sourcePackage;
    }

    public String getSourceClass() {
        return sourceClass;
    }

    @Override
    public String getTargetPackage() {
        return targetPackage;
    }
}
