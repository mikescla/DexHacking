package InjectionMgmt;

/**
 * Convenience class containing information about where and what calls to inject
 *
 * @author Michele Scalas
 */
public class InjectionData {
    private final int classIndex;
    private final int methodIndex;
    private final String featureName;

    public InjectionData(int classIndex, int methodIndex, String featureName) {
        this.classIndex = classIndex;
        this.methodIndex = methodIndex;
        this.featureName = featureName;
    }

    public int getClassIndex() {
        return classIndex;
    }

    public int getMethodIndex() {
        return methodIndex;
    }

    public String getFeatureName() {
        return featureName;
    }
}
