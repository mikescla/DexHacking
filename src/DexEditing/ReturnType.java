/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package DexEditing;

import java.util.HashMap;
import java.util.Map;

/**
 * @author fabri
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
