/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package DexEditing;

import static javafx.application.Platform.exit;

/**
 *
 * @author fabri
 */
class ReturnType {
    private static String returnString = "S";
    private static String returnInt = "I";
    private static String returnVoid = "V";

    private static String getReturnString() {
        return returnString;
    }

    private static String getReturnInt() {
        return returnInt;
    }

    private static String getReturnVoid() {
        return returnVoid;
    }
    
        
    protected static String getReturnTypeFromString(String returnType){
        switch(returnType.toLowerCase()){
            case "string": return getReturnString();
            case "int": return getReturnInt();
            case "void": return getReturnVoid();
            default:
                System.err.println("ReturnType not recognized!!");
                exit();
        }
        return null;
    }
}
