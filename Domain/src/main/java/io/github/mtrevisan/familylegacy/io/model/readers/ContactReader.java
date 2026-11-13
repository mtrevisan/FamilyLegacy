/**
 * Copyright (c) 2026 Mauro Trevisan
 * <p>
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * <p>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.familylegacy.io.model.readers;

import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import org.apache.commons.lang3.StringUtils;

import java.util.Locale;
import java.util.Set;


/**
 * Handler for CONTACT records.
 * <p>
 * Structure:
 * <pre>
 * // Machine-readable contact information for a person, organization, repository, submitter, or other entity.
 * struct ContactStructure {
 *   value: Text   // a phone number, an electronic address such as an email (RFC 5322), a web page address (RFC 3986), or any other type of contact address
 *   type?: enum {
 *     email,      // Email address.
 *     phone,      // Voice telephone number.
 *     mobile,     // Mobile telephone number.
 *     fax,        // Fax number.
 *     website,    // Website or homepage.
 *     blog,       // Blog or personal publication site.
 *     social,     // Social media profile or account.
 *     postal,     // Postal or physical address.
 *     messaging   // Messaging application endpoint (e.g. WhatsApp, Signal, Telegram).
 *   } | Text
 *   name?: struct {
 *     value: Text                  // the name of the person associated with this contact
 *     variant*: TextValueVariant   // alternative phonetic, transliterated, or transcribed representations
 *   }
 *   remark?: Text
 *   privacy?: PrivacyStructure
 *   audit: AuditStructure
 * }
 * </pre>
 */
public final class ContactReader{

	public static final String TYPE = "CONTACT";

	public static final String TAG_VALUE = "value";
	public static final String TAG_TYPE = "type";
	public static final String TAG_NAME = "name";
	public static final String TAG_VARIANT = "variant";
	public static final String TAG_REMARK = "remark";
	public static final String TAG_PRIVACY = "privacy";
	public static final String TAG_AUDIT = "audit";

	public static final String TAG_NAME_VALUE = FLEFRecordHelper.composePath(TAG_NAME, TAG_VALUE);
	public static final String TAG_NAME_VARIANT = FLEFRecordHelper.composePath(TAG_NAME, TAG_VARIANT);

	public static final String ENUM_TYPE_EMAIL = "email";
	public static final String ENUM_TYPE_PHONE = "phone";
	public static final String ENUM_TYPE_MOBILE = "mobile";
	public static final String ENUM_TYPE_FAX = "fax";
	public static final String ENUM_TYPE_WEBSITE = "website";
	public static final String ENUM_TYPE_BLOG = "blog";
	public static final String ENUM_TYPE_SOCIAL = "social";
	public static final String ENUM_TYPE_POSTAL = "postal";
	public static final String ENUM_TYPE_MESSAGING = "messaging";
	public static final String[] TYPES = {
		ENUM_TYPE_EMAIL, ENUM_TYPE_PHONE, ENUM_TYPE_MOBILE, ENUM_TYPE_FAX, ENUM_TYPE_WEBSITE, ENUM_TYPE_BLOG, ENUM_TYPE_SOCIAL, ENUM_TYPE_POSTAL, ENUM_TYPE_MESSAGING
	};


	private ContactReader(){}

	private static final Set<String> SOCIAL_HOSTS = Set.of("facebook.com", "fb.com", "instagram.com", "twitter.com", "x.com", "linkedin.com",
		"tiktok.com", "youtube.com", "pinterest.com", "threads.net", "mastodon.social", "reddit.com");
	private static final Set<String> MESSAGING_HOSTS = Set.of("wa.me", "whatsapp.com", "t.me", "telegram.me", "telegram.org", "signal.me",
		"skype.com", "m.me");
	private static final Set<String> BLOG_HOSTS = Set.of("blogspot.com", "wordpress.com", "medium.com", "tumblr.com", "substack.com",
		"blogger.com");
	private static final Set<String> STREET_WORDS = Set.of("via", "viale", "piazza", "piazzale", "corso", "strada", "vicolo", "largo",
		"borgo", "contrada", "località", "localita", "street", "road", "avenue", "lane", "drive", "square",
		"strasse", "straße", "platz", "rue", "boulevard", "calle", "avenida", "plaza", "rua", "box");


	/**
	 * Guesses the type of a contact from its value: an e-mail address, a telephone number, a web address (a website, a social network,
	 * a messaging service or a blog, as its host tells) or a postal address.
	 *
	 * @param value	The value of the contact.
	 * @return	The type (one of {@link #TYPES}), or {@code null} if the value is not recognized.
	 */
	public static String inferType(final String value){
		final String text = StringUtils.trimToEmpty(value);
		if(text.isEmpty())
			return null;

		final String lower = text.toLowerCase(Locale.ROOT);
		if(lower.startsWith("mailto:"))
			return ENUM_TYPE_EMAIL;
		if(lower.startsWith("tel:") || lower.startsWith("callto:"))
			return ENUM_TYPE_PHONE;
		if(lower.startsWith("fax:"))
			return ENUM_TYPE_FAX;
		if(lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("www."))
			return typeOfWebAddress(lower);
		if(isEmailAddress(text))
			return ENUM_TYPE_EMAIL;
		if(isPhoneNumber(text))
			return ENUM_TYPE_PHONE;
		if(isPostalAddress(lower))
			return ENUM_TYPE_POSTAL;
		return null;
	}

	/**
	 * Fixes a contact type that contradicts the value (for example, an e-mail address written as a phone).
	 * <p>
	 * A fax is never changed to a phone, as a fax number looks like a phone number, and postal addresses are left alone.
	 *
	 * @param value	The value of the contact.
	 * @param declaredType	The type the source file declares.
	 * @return	The type to write.
	 */
	public static String correctType(final String value, final String declaredType){
		if(ENUM_TYPE_POSTAL.equals(declaredType))
			return declaredType;

		final String inferred = inferType(value);
		if(inferred == null || inferred.equals(declaredType) || ENUM_TYPE_POSTAL.equals(inferred))
			return declaredType;
		if(ENUM_TYPE_PHONE.equals(inferred) && ENUM_TYPE_FAX.equals(declaredType))
			return declaredType;
		return inferred;
	}

	private static String typeOfWebAddress(final String lowerAddress){
		String host = lowerAddress;
		final int scheme = host.indexOf("://");
		if(scheme >= 0)
			host = host.substring(scheme + 3);
		host = StringUtils.substringBefore(StringUtils.substringBefore(StringUtils.substringBefore(host, '/'), '?'), '#');
		if(host.startsWith("www."))
			host = host.substring(4);

		if(hostIn(host, SOCIAL_HOSTS))
			return ENUM_TYPE_SOCIAL;
		if(hostIn(host, MESSAGING_HOSTS))
			return ENUM_TYPE_MESSAGING;
		if(hostIn(host, BLOG_HOSTS))
			return ENUM_TYPE_BLOG;
		return ENUM_TYPE_WEBSITE;
	}

	/** Whether the host is one of the domains, or a subdomain of one of them. */
	private static boolean hostIn(final String host, final Set<String> domains){
		for(final String domain : domains)
			if(host.equals(domain) || host.endsWith("." + domain))
				return true;
		return false;
	}

	private static boolean isEmailAddress(final String text){
		final int at = text.indexOf('@');
		if(at < 1 || at != text.lastIndexOf('@') || StringUtils.containsWhitespace(text))
			return false;

		final String domain = text.substring(at + 1);
		return (domain.indexOf('.') > 0 && !domain.startsWith(".") && !domain.endsWith("."));
	}

	private static boolean isPhoneNumber(final String text){
		final boolean international = text.startsWith("+");
		int digits = 0;
		for(int i = 0, length = text.length(); i < length; i ++){
			final char c = text.charAt(i);
			if(c >= '0' && c <= '9')
				digits ++;
			else if(c == '+' && i == 0 || c == ' ' || c == '-' || c == '(' || c == ')' || c == '.' && international){}
			else
				return false;
		}
		return (digits >= 6 && digits <= 15);
	}

	private static boolean isPostalAddress(final String lowerText){
		boolean digit = false;
		for(int i = 0, length = lowerText.length(); i < length && !digit; i ++)
			digit = Character.isDigit(lowerText.charAt(i));
		if(!digit)
			return false;

		for(final String word : StringUtils.split(lowerText, " ,.;:\n\r\t"))
			if(STREET_WORDS.contains(word))
				return true;
		return false;
	}


	public static String extractValue(final FLEFRecord record){
		return FLEFRecordHelper.getChildValue(record, TAG_VALUE);
	}

	public static String extractType(final FLEFRecord record){
		return FLEFRecordHelper.getChildValue(record, TAG_TYPE);
	}

	public static String extractName(final FLEFRecord record){
		return FLEFRecordHelper.getChildValue(record, TAG_NAME);
	}

}
