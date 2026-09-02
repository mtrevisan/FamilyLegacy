package io.github.mtrevisan.familylegacy.v2.ui.components.individual;

import io.github.mtrevisan.familylegacy.v2.ui.helpers.ParsedGenealogicalDate;


/**
 * Represents a single event (birth or death) with date and place.
 *
 * @param type        `birth` or `death`
 * @param rawDate     original date string (for display)
 * @param approximate whether the date is approximate
 * @param place       place name or original_text
 * @param deathCause  cause of death
 */
public record EventInfo(String type, String rawDate, ParsedGenealogicalDate date, boolean approximate, String place, String deathCause){}
