package io.github.mtrevisan.familylegacy.v2.gedcom.utils;

import org.apache.commons.lang3.StringUtils;


/**
 * Utility to normalize GEDCOM local identifiers.
 */
public final class IDNormalizer{

	private IDNormalizer(){
	}

	/**
	 * Removes leading/trailing '@' characters and trims the result.
	 * Example: "@I7@" -> "I7"
	 */
	public static String clean(String id){
		if(id == null) return null;
		return id.replace("@", StringUtils.EMPTY).trim();
	}

	public static boolean isValidIdFormat(String id){
		// Simple check: alphanumeric, no spaces or dashes (but dashes are allowed per spec, but we avoid)
		// We'll accept letters and digits only.
		return id != null && id.matches("^[A-Za-z][A-Za-z0-9]*$");
	}

}
