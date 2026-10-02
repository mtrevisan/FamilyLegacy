package io.github.mtrevisan.familylegacy.io.model.readers;

import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecordHelper;


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
 *   note?: Text
 *   privacy?: PrivacyStructure
 *   audit: AuditStructure
 * }
 * </pre>
 */
public final class ContactReader{

	public static final String TAG_VALUE = "value";
	public static final String TAG_TYPE = "type";
	public static final String TAG_NAME = "name";
	public static final String TAG_VARIANT = "variant";
	public static final String TAG_NOTE = "note";
	public static final String TAG_PRIVACY = "privacy";
	public static final String TAG_AUDIT = "audit";

	public static final String TAG_NAME_VALUE = FLEFRecordHelper.composePath(TAG_NAME, TAG_VALUE);
	public static final String TAG_NAME_VARIANT = FLEFRecordHelper.composePath(TAG_NAME, TAG_VARIANT);

	public static final String[] TYPES = {
		"email", "phone", "mobile", "fax", "website", "blog", "social", "postal", "messaging"
	};


	private ContactReader(){}


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
