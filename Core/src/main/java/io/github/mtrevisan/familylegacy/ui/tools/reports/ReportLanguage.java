package io.github.mtrevisan.familylegacy.ui.tools.reports;

import java.util.Locale;


/**
 * Languages supported by the report generator.
 *
 * <p>Each constant carries a {@link Locale} that drives locale-sensitive
 * formatting (numbers, dates, ICU plural rules) as well as a human-readable
 * display name shown in the UI.</p>
 *
 * <p>Locales are obtained through {@link Locale#of} or the standard
 * constants, never through the {@code new Locale(...)} constructors
 * (deprecated since Java 19). For simple languages the single-argument
 * {@code Locale.of(language)} is used; for regional variants the
 * two-argument overload is used.</p>
 *
 * <p><b>RTL languages</b> (Arabic, Hebrew, Urdu, Persian): the
 * {@code .properties} file is UTF-8, so the text is stored literally;
 * right-to-left rendering is a concern of the output format, not of the
 * bundle.</p>
 *
 * <p><b>Plural forms</b>: Arabic declares six CLDR categories
 * ({@code zero/one/two/few/many/other}), Slavic languages declare three
 * ({@code one/few/many}), Hindi/Urdu/Bengali declare two, and Chinese,
 * Japanese, Korean, Thai, Indonesian, Turkish, Persian and Vietnamese
 * declare only {@code other}. The {@code .properties} files handle this
 * directly; the code does not need to know.</p>
 */
public enum ReportLanguage{

	/* ----- Western Europe ----- */

	ENGLISH(Locale.ENGLISH, "English"),
	ENGLISH_GB(Locale.UK, "English (UK)"),
	ENGLISH_US(Locale.US, "English (US)"),
	ITALIAN(Locale.ITALIAN, "Italiano"),
	GERMAN(Locale.GERMAN, "Deutsch"),
	FRENCH(Locale.FRENCH, "Français"),
	SPANISH(Locale.of("es"), "Español"),
	PORTUGUESE(Locale.of("pt"), "Português"),
	DUTCH(Locale.of("nl"), "Nederlands"),

	/* ----- Northern Europe ----- */

	SWEDISH(Locale.of("sv"), "Svenska"),
	DANISH(Locale.of("da"), "Dansk"),
	NORWEGIAN(Locale.of("nb"), "Norsk bokmål"),
	FINNISH(Locale.of("fi"), "Suomi"),

	/* ----- Central and Eastern Europe ----- */

	POLISH(Locale.of("pl"), "Polski"),
	CZECH(Locale.of("cs"), "Čeština"),
	HUNGARIAN(Locale.of("hu"), "Magyar"),
	ROMANIAN(Locale.of("ro"), "Română"),
	RUSSIAN(Locale.of("ru"), "Русский"),
	UKRAINIAN(Locale.of("uk"), "Українська"),
	GREEK(Locale.of("el"), "Ελληνικά"),

	/* ----- Middle East ----- */

	ARABIC(Locale.of("ar"), "العربية"),
	HEBREW(Locale.of("he"), "עברית"),
	TURKISH(Locale.of("tr"), "Türkçe"),
	PERSIAN(Locale.of("fa"), "فارسی"),

	/* ----- South Asia ----- */

	HINDI(Locale.of("hi"), "हिन्दी"),
	BENGALI(Locale.of("bn"), "বাংলা"),
	URDU(Locale.of("ur"), "اردو"),
	TAMIL(Locale.of("ta"), "தமிழ்"),
	TELUGU(Locale.of("te"), "తెలుగు"),
	MARATHI(Locale.of("mr"), "मराठी"),
	GUJARATI(Locale.of("gu"), "ગુજરાતી"),
	PUNJABI(Locale.of("pa"), "ਪੰਜਾਬੀ"),

	/* ----- Southeast Asia ----- */

	INDONESIAN(Locale.of("id"), "Bahasa Indonesia"),
	VIETNAMESE(Locale.of("vi"), "Tiếng Việt"),
	THAI(Locale.of("th"), "ไทย"),
	TAGALOG(Locale.of("tl"), "Tagalog"),

	/* ----- East Asia ----- */

	CHINESE_SIMPLIFIED(Locale.of("zh", "CN"), "简体中文"),
	CHINESE_TRADITIONAL(Locale.of("zh", "TW"), "繁體中文"),
	JAPANESE(Locale.of("ja"), "日本語"),
	KOREAN(Locale.of("ko"), "한국어"),

	/* ----- Africa ----- */

	SWAHILI(Locale.of("sw"), "Kiswahili"),
	HAUSA(Locale.of("ha"), "Hausa");


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
