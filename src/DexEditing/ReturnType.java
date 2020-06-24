package DexEditing;

import java.util.HashMap;
import java.util.Map;

/**
 * Convenience class to store the mapping between primitive Java types and
 * the associated smali identifier
 *
 * @author fabri
 * @author Michele Scalas
 */
public class ReturnType {
    static final Map<String, String> primitiveMapping = new HashMap<>() {{
        put("int", "I");
        put("boolean", "Z");
        put("byte", "B");
        put("short", "S");
        put("char", "C");
        put("long", "J");
        put("float", "F");
        put("double", "D");
        put("void", "V");
    }};

    public static String getReturnTypeFromString(String inputType) {
        return primitiveMapping.getOrDefault(inputType, null);
    }
}
