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
package io.github.mtrevisan.familylegacy.flef.persistence.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;


@Entity(name = "localized_person_name")
public class LocalizedPersonNameEntity{

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "localized_person_name_generator")
	@SequenceGenerator(name = "localized_person_name_generator", allocationSize = 1, sequenceName = "localized_person_name_sequence")
	@Column(name = "id")
	private Long id;

	//A localized (primary, that is the proper name) name.
	@Column(name = "personal_name")
	private String personalName;

	//A localized (seconday, that is everything that is not a proper name, like a surname) name.
	@Column(name = "family_name")
	private String familyName;

	//Locale of the name as defined in ISO 639 (https://en.wikipedia.org/wiki/ISO_639).
	@Column(name = "locale")
	private String locale;

	//Can be "original", "transliteration", or "translation".
	@Column(name = "type")
	private String type;

	//Indicates the system used in transcript the text to the romanized variation (ex. "IPA", "Wade-Giles", "hanyu pinyin", "wāpuro rōmaji",
	// "kana", "hangul").
	@Column(name = "transcription")
	private String transcription;

	//Type of transcription (usually "romanized", but it can be "anglicized", "cyrillized", "francized", "gairaigized", "latinized", etc).
	@Column(name = "transcription_type")
	private String transcriptionType;

	//The ID of the referenced record in the table.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "person_name_id", referencedColumnName = "id", nullable = false)
	private PersonNameEntity personName;


	public Long getID(){
		return id;
	}


	@Override
	public boolean equals(Object o){
		if(this == o)
			return true;
		if(!(o instanceof final LocalizedPersonNameEntity other))
			return false;
		return (id != null && id.equals(other.getID()));
	}

	@Override
	public int hashCode(){
		return 31;
	}

}
