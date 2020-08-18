package com.thed.utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ListUtil {

    public static Set<String> getSet(String value, String splitStr) {
        if(value == null || splitStr == null) {
            return new HashSet<>();
        }
        return new HashSet<>(Arrays.asList(value.split(splitStr)));
    }

}