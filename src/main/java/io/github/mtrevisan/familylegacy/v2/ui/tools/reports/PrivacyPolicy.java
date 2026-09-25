package io.github.mtrevisan.familylegacy.v2.ui.tools.reports;


/**
 * How the report treats records marked with a {@code privacy} block.
 *
 * <p>The FLEF protocol defines three privacy levels: {@code public},
 * {@code restricted} and {@code confidential}. This enum captures the three
 * combinations the report supports.</p>
 */
public enum PrivacyPolicy{

	/** Show every record, regardless of its declared privacy level. */
	SHOW_ALL,

	/** Hide {@code confidential} records; show {@code restricted} ones. */
	HIDE_CONFIDENTIAL,

	/** Hide both {@code confidential} and {@code restricted} records. */
	HIDE_RESTRICTED_AND_CONFIDENTIAL

}
