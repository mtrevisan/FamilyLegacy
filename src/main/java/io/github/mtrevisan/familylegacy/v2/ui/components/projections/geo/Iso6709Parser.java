/**
 * Copyright (c) 2026 Mauro Trevisan
 * ... (license header unchanged)
 */
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.geo;

import org.apache.commons.lang3.StringUtils;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for ISO 6709 geographic coordinate strings.
 * <p>
 * Supports the three common representations:
 * <ul>
 *   <li>{@code ±DD.DDDD±DDD.DDDD/} — decimal degrees</li>
 *   <li>{@code ±DDMM.MMM±DDDMM.MMM/} — degrees and minutes</li>
 *   <li>{@code ±DDMMSS.S±DDDMMSS.S/} — degrees, minutes, seconds</li>
 * </ul>
 * plus an optional altitude suffix ({@code +AAAA.A/} or {@code -AAAA.A/}).
 * Cardinal direction letters (N/S/E/W) are also accepted and translated
 * into the sign of the corresponding component.
 */
public final class Iso6709Parser{

	private static final Pattern DECIMAL = Pattern.compile(
		"([+-]?\\d+(?:\\.\\d+)?)([NnSs]?)([+-]?\\d+(?:\\.\\d+)?)([EeWw]?)");
	private static final Pattern DMS = Pattern.compile(
		"([+-]?)(\\d{1,2})(\\d{2})(\\d{2}(?:\\.\\d+)?)([NnSs]?)([+-]?)(\\d{1,3})(\\d{2})(\\d{2}(?:\\.\\d+)?)([EeWw]?)");

	private Iso6709Parser(){}


	/**
	 * Parses an ISO 6709 coordinate string.
	 *
	 * @param raw the string to parse
	 * @return the parsed coordinate, or {@code null} if the input is
	 *         {@code null} or unparsable
	 */
	public static GeoCoordinate parse(final String raw){
		if(raw == null || raw.isBlank())
			return null;

		final String cleaned = raw.trim()
			.replace("/", StringUtils.EMPTY);
		try{
			// Try DMS first, because it is more specific.
			final Matcher dms = DMS.matcher(cleaned);
			if(dms.matches())
				return parseDms(dms, raw);

			final Matcher dec = DECIMAL.matcher(cleaned);
			if(dec.matches())
				return parseDecimalOrMinutes(dec, raw);
		}
		catch(final NumberFormatException ignored){
			return null;
		}
		return null;
	}


	private static GeoCoordinate parseDecimalOrMinutes(final Matcher m, final String original){
		final String latRaw = m.group(1);
		final String latCard = m.group(2);
		final String lonRaw = m.group(3);
		final String lonCard = m.group(4);

		// Detect degrees+minutes form: no decimal separator and more than
		// 3 digits in the integer part (e.g. 4041 for 40°41').
		final boolean latIsDm = (latRaw.indexOf('.') < 0 && Math.abs(parseDouble(latRaw)) >= 1000);
		final boolean lonIsDm = (lonRaw.indexOf('.') < 0 && Math.abs(parseDouble(lonRaw)) >= 10000);

		final double latitude = (latIsDm? parseDegreesMinutes(latRaw, 2): parseDouble(latRaw));
		final double longitude = (lonIsDm? parseDegreesMinutes(lonRaw, 3): parseDouble(lonRaw));

		final double signedLat = applyCardinal(latitude, latCard, true);
		final double signedLon = applyCardinal(longitude, lonCard, false);

		final GeoCoordinatePrecision precision = (latIsDm || lonIsDm
			? GeoCoordinatePrecision.MINUTES
			: GeoCoordinatePrecision.DEGREES);

		return new GeoCoordinate(signedLat, signedLon, null, original, precision);
	}

	private static GeoCoordinate parseDms(final Matcher m, final String original){
		final String latSign = m.group(1);
		final int latDeg = Integer.parseInt(m.group(2));
		final int latMin = Integer.parseInt(m.group(3));
		final double latSec = parseDouble(m.group(4));
		final String latCard = m.group(5);

		final String lonSign = m.group(6);
		final int lonDeg = Integer.parseInt(m.group(7));
		final int lonMin = Integer.parseInt(m.group(8));
		final double lonSec = parseDouble(m.group(9));
		final String lonCard = m.group(10);

		double latitude = latDeg + latMin / 60. + latSec / 3600.;
		double longitude = lonDeg + lonMin / 60. + lonSec / 3600.;

		if("-".equals(latSign))
			latitude = -latitude;
		if("-".equals(lonSign))
			longitude = -longitude;

		latitude = applyCardinal(latitude, latCard, true);
		longitude = applyCardinal(longitude, lonCard, false);

		return new GeoCoordinate(latitude, longitude, null, original, GeoCoordinatePrecision.SECONDS);
	}


	private static double parseDegreesMinutes(final String raw, final int degreeDigits){
		final boolean negative = raw.startsWith("-");
		final String digits = raw.replaceAll("[+-]", StringUtils.EMPTY);
		if(digits.length() < degreeDigits + 2)
			return parseDouble(raw);
		final int degrees = Integer.parseInt(digits.substring(0, degreeDigits));
		final double minutes = Double.parseDouble(digits.substring(degreeDigits));
		final double value = degrees + minutes / 60.;
		return (negative? -value: value);
	}

	private static double applyCardinal(final double value, final String cardinal, final boolean isLatitude){
		if(cardinal == null || cardinal.isEmpty())
			return value;
		final char c = Character.toUpperCase(cardinal.charAt(0));
		final boolean negative = (isLatitude && c == 'S') || (!isLatitude && c == 'W');
		return (negative? -Math.abs(value): Math.abs(value));
	}

	private static double parseDouble(final String s){
		return Double.parseDouble(s.trim());
	}

}
