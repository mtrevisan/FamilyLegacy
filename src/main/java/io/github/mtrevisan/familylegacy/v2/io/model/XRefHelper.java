package io.github.mtrevisan.familylegacy.v2.io.model;

import org.apache.commons.lang3.StringUtils;

import java.util.Objects;


public class XRefHelper{

	private static final String XREF_PREFIX = "@";
	private static final String XREF_SUFFIX = "@";
	private static final String XREF_VOID = XREF_PREFIX + "VOID" + XREF_SUFFIX;


	private XRefHelper(){}


	/**
	 * Formats a raw ID into an XREF reference (e.g., "N123" -> "@N123@").
	 * If the ID is already formatted or null/blank, it is handled safely.
	 *
	 * @param xref	The raw ID to format.
	 * @return	The formatted XREF string, or {@code null} if the input is blank.
	 */
	public static String formatXRef(final String xref){
		if(StringUtils.isEmpty(xref))
			return null;

		return (isReference(xref)
			? xref
			: XREF_PREFIX + xref + XREF_SUFFIX);
	}

	/**
	 * Extracts the raw ID from an XREF reference (e.g., "@N123@" -> "N123").
	 *
	 * @param xref	The XREF string to strip.
	 * @return	The unformatted raw ID, or the original string if not an XREF.
	 */
	public static String extractXRef(final String xref){
		return (isReference(xref)
			? xref.substring(XREF_PREFIX.length(), xref.length() - XREF_SUFFIX.length())
			: null);
	}

	/**
	 * Checks whether this record's value is a reference to another record
	 * (i.e. wrapped in {@code @...@}, but not the special {@code @VOID@} constant).
	 *
	 * @param value	The value.
	 */
	public static boolean isReference(final String value){
		return (value != null && value.startsWith(XREF_PREFIX) && value.endsWith(XREF_SUFFIX)
			&& value.length() >= XREF_PREFIX.length() + XREF_SUFFIX.length()
			&& !isVoidReference(value));
	}

	/**
	 * Checks whether this record's value is the special {@code @VOID@} constant.
	 *
	 * @param value	The value.
	 */
	public static boolean isVoidReference(final String value){
		return XREF_VOID.equals(value);
	}

	/**
	 * Adds a child record representing a reference (XREF) to another record.
	 * Automatically wraps the target ID with '@' delimiters.
	 *
	 * @param parent	The parent record.
	 * @param tag	The tag for the child record (e.g., "NOTE", "INDIVIDUAL").
	 * @param targetId	The raw target ID to reference.
	 * @return	The created child record, or {@code null} if input parameters are invalid.
	 */
	public static FLEFRecord addReferenceChild(final FLEFRecord parent, final String tag, final String targetId){
		if(parent == null || tag == null || targetId == null)
			return null;

		final FLEFRecord child = FLEFRecord.createChildWithValue(tag, formatXRef(targetId));
		parent.addChild(child);
		return child;
	}

	/**
	 * Generates a new unique ID for a record type.
	 *
	 * @param model	The FLEF model.
	 * @param type	The record type (e.g., "INDIVIDUAL", "FAMILY", "EVENT").
	 * @param prefix	The ID prefix (e.g., "I", "F", "E").
	 * @return	A new unique ID.
	 */
	public static String generateNewId(final FLEFModel model, final String type, final String prefix){
		if(model == null || type == null || prefix == null)
			return prefix + "1";

		final int max = model.getRecordsByType(type).stream()
			.map(FLEFRecord::getId)
			.filter(Objects::nonNull)
			.filter(id -> id.startsWith(prefix))
			.mapToInt(id -> {
				try{
					return Integer.parseInt(id.substring(prefix.length()));
				}
				catch(final NumberFormatException ignored){
					return 0;
				}
			})
			.max()
			.orElse(0);

		return prefix + (max + 1);
	}

}
