/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package DexEditing;

import static javafx.application.Platform.exit;
import org.jf.dexlib2.AccessFlags;



/**
 *
 * @author fabri
 */
public class AccessFlag {
    
    protected static int getAccessFlagPublic() {
        return AccessFlags.PUBLIC.getValue(); //1
    }

    protected static int getAccessFlagPrivate() {
        return AccessFlags.PRIVATE.getValue(); //4
    }
    
    protected static int getAccessFlagProtected() {
        return AccessFlags.PROTECTED.getValue(); //2
    }
    
    protected static int getAccessFlagConstructor() {
        return AccessFlags.CONSTRUCTOR.getValue(); //65536
    }
        
    protected static int getAccessFlagFromString(String accessType){
        switch(accessType.toLowerCase()){
            case "public": return AccessFlag.getAccessFlagPublic();
            case "protected": return AccessFlag.getAccessFlagProtected();
            case "private": return AccessFlag.getAccessFlagPrivate();
            default:
                System.err.println("AccessFlag not recognized!!");
                exit();
        }
        return 0;
    }
}
