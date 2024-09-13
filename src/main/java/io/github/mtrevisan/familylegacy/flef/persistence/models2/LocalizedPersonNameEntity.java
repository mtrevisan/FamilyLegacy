/**
 * Copyright (c) 2024 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.flef.persistence.models2;

import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;


@NodeEntity(label = "LocalizedPersonName")
public class LocalizedPersonNameEntity extends AbstractEntity{

	@Id
	@GeneratedValue
	private Long id;

	//A localized (primary, that is the proper name) name.
	private String personalName;

	//A localized (seconday, that is everything that is not a proper name, like a surname) name.
	private String familyName;

	//Locale of the name as defined in ISO 639 (https://en.wikipedia.org/wiki/ISO_639).
	private String locale;

	//Can be "original", "transliteration", or "translation".
	private String type;

	//Indicates the system used in transcript the text to the romanized variation (ex. "IPA", "Wade-Giles", "hanyu pinyin", "wāpuro rōmaji",
	// "kana", "hangul").
	private String transcription;

	//Type of transcription (usually "romanized", but it can be "anglicized", "cyrillized", "francized", "gairaigized", "latinized", etc).
	private String transcriptionType;

	//The ID of the referenced record in the table.
	@Relationship(type = "transcription_of", direction = Relationship.Direction.INCOMING)
	private PersonNameEntity personName;


	@Override
	public Long getID(){
		return id;
	}

}
