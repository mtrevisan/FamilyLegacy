package io.github.mtrevisan.familylegacy.v2.ui.tools.reports;

import java.util.Locale;


/**
 * Languages supported by the report generator.
 *
 * <p>Each constant carries a {@link Locale} that can be used for
 * locale-sensitive formatting (numbers, dates) as well as a human-readable
 * display name shown in the UI.</p>
 */
public enum ReportLanguage{

	/** English (default). */
	ENGLISH(Locale.ENGLISH, "English"),
	/** Italian. */
	ITALIAN(Locale.ITALIAN, "Italiano");


	private final Locale locale;
	private final String displayName;


	ReportLanguage(final Locale locale, final String displayName){
		this.locale = locale;
		this.displayName = displayName;
	}


	public Locale locale(){
		return locale;
	}

	public String displayName(){
		return displayName;
	}

	@Override
	public String toString(){
		return displayName;
	}

}
