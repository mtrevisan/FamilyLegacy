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
package io.github.mtrevisan.familylegacy.io.model.readers.names;


import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.readers.TextValueVariantReader;
import org.apache.commons.lang3.StringUtils;


/**
 * A phonetic or transcription variant of a name or name part.
 * <p>
 * Mirrors the FLEF {@code TextValueVariant} oneof. The {@code kind} field
 * distinguishes the two alternatives:
 * <ul>
 *   <li>{@code "phonetic"} — a phonetic or phonemic representation, with
 *       a {@code system} such as {@code IPA} or {@code canIPA};</li>
 *   <li>{@code "transcription"} — a transliteration or transcription into
 *       another writing system, with a {@code system} (e.g. {@code rōmaji},
 *       {@code pinyin}, {@code iso9}) and an optional {@code type}
 *       (e.g. {@code romanized}, {@code anglicized}, {@code hellenized}).</li>
 * </ul>
 * The record is immutable. Empty fields are normalized to empty strings.
 */
public record TextValueVariant(String kind, String system, String type, String value){

	public TextValueVariant{
		if(kind == null)
			kind = StringUtils.EMPTY;
		if(system == null)
			system = StringUtils.EMPTY;
		if(type == null)
			type = StringUtils.EMPTY;
		if(value == null)
			value = StringUtils.EMPTY;
	}


	public static TextValueVariant create(final FLEFRecord kindChild){
		final String kind = kindChild.getTag();
		if(TextValueVariantReader.isPhonetic(kindChild.getTag())){
			final String system = TextValueVariantReader.extractPhoneticSystem(kindChild);
			final String value = TextValueVariantReader.extractPhoneticValue(kindChild);
			return new TextValueVariant(kind, system, null, value);
		}
		else if(TextValueVariantReader.isTranscription(kindChild.getTag())){
			final String system = TextValueVariantReader.extractTranscriptionSystem(kindChild);
			final String type = TextValueVariantReader.extractTranscriptionType(kindChild);
			final String value = TextValueVariantReader.extractTranscriptionValue(kindChild);
			return new TextValueVariant(kind, system, type, value);
		}
		return null;
	}


	/**
	 * Returns whether this variant is a phonetic representation.
	 *
	 * @return {@code true} if the kind is {@code PHONETIC}
	 */
	public boolean isPhonetic(){
		return TextValueVariantReader.isPhonetic(kind);
	}

	/**
	 * Returns whether this variant is a transcription or transliteration.
	 *
	 * @return {@code true} if the kind is {@code TRANSCRIPTION}
	 */
	public boolean isTranscription(){
		return TextValueVariantReader.isTranscription(kind);
	}

	/**
	 * Returns a compact display label for the variant, combining the
	 * system and the type when both are present.
	 *
	 * @return the display label, never {@code null}
	 */
	public String displayLabel(){
		final StringBuilder sb = new StringBuilder();
		if(!system.isEmpty())
			sb.append(system);
		if(!type.isEmpty()){
			if(!sb.isEmpty())
				sb.append(" / ");
			sb.append(type);
		}
		if(sb.isEmpty())
			sb.append(kind);
		return sb.toString();
	}

}
