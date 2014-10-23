package com.superliminal.util;


import java.io.File;
import java.net.URISyntaxException;

/**
 * A collection of generally useful platform-specific utility methods.
 *
 * Copyright 2005 - Superliminal Software
 * @author Melinda Green
 */
public class PlatformUtils {
    // to disallow instantiation
    private PlatformUtils(){}
    
	public static String getBinDir() {
//		try {
//			String here = new File(PlatformUtils.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).toString();
//			return here.endsWith(".jar") ? here.substring(0, here.lastIndexOf(File.separator)) : here;
//		} catch (URISyntaxException e) {
//			e.printStackTrace();
//		}
		return null;
    }

}
