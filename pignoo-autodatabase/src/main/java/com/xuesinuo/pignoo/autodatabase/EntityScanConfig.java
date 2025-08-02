package com.xuesinuo.pignoo.autodatabase;

import lombok.Data;

@Data
public class EntityScanConfig {

    public static enum buildMode {
        CAREFULLY, USABILITY
    }

    boolean hasChildPackages;
    boolean annClassOnly;
    String[] packages;

}
