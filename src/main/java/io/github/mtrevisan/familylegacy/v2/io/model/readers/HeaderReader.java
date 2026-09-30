package io.github.mtrevisan.familylegacy.v2.io.model.readers;


import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;


/**
 * Handler for HEADER records.
 * <p>
 * Structure:
 * <pre>
 * struct Header {
 *   protocol: struct {
 *     name: Text        // the name of the protocol
 *     version: SemVer   // the version of the protocol. It is defined and changed by the creators of the product.
 *   }
 *   source?: struct {
 *     name?: Text        // the human-readable name of the software product that produced this file
 *     version?: SemVer   // the version of the software product. It is defined and changed by the creators of the product.
 *     organization?: Text   // the name of the organization or person that produced or commissioned the product
 *   }
 *   date: Date         // the date this source was created
 *   copyright?: Text   // a copyright statement needed to protect the copyrights of the submitter of this file
 *   submitter?: struct {
 *     contact*: ContactStructure   // contact information for the submitter (phone, email, web, etc.)
 *     note?: Text                  // any notes related to the submitter
 *   }
 *   scope?: Text       // a brief description of the file's genealogical scope (e.g., "Ancestors of John Doe")
 * }
* </pre>
 */
public final class HeaderReader{

	private static final String TAG_PROTOCOL = "protocol";
	private static final String TAG_NAME = "name";
	public static final String TAG_PROTOCOL_NAME = FLEFRecordHelper.composePath(TAG_PROTOCOL, TAG_NAME);
	private static final String TAG_VERSION = "version";
	public static final String TAG_PROTOCOL_VERSION = FLEFRecordHelper.composePath(TAG_PROTOCOL, TAG_VERSION);
	private static final String TAG_SOURCE = "source";
	public static final String TAG_SOURCE_NAME = FLEFRecordHelper.composePath(TAG_SOURCE, TAG_NAME);
	public static final String TAG_SOURCE_VERSION = FLEFRecordHelper.composePath(TAG_SOURCE, TAG_VERSION);
	public static final String TAG_SOURCE_ORGANIZATION = "organization";
	public static final String TAG_DATE = "date";
	public static final String TAG_COPYRIGHT = "copyright";
	private static final String TAG_SUBMITTER = "submitter";
	public static final String TAG_SUBMITTER_CONTACT = FLEFRecordHelper.composePath(TAG_SUBMITTER, "contact");
	public static final String TAG_SUBMITTER_NOTE = FLEFRecordHelper.composePath(TAG_SUBMITTER, "note");
	public static final String TAG_SCOPE = "scope";


	private HeaderReader(){}

}
