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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;


//Transcriptions and transliterations of the name can be attached through a localized text (with type "name").
// Additional media can be attached.
@Entity(name = "place")
@Table(uniqueConstraints = @UniqueConstraint(name = "unique_identifier", columnNames = "identifier"))
public class PlaceEntity{

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "place_generator")
	@SequenceGenerator(name = "place_generator", allocationSize = 1, sequenceName = "place_sequence")
	@Column(name = "id")
	private Long id;

	//An identifier for the place (must be unique).
	@Column(name = "identifier", nullable = false)
	private String identifier;

	//A verbatim copy of the name written in the original language.
	@Column(name = "name", nullable = false)
	private String name;

	//Locale of the name as defined in ISO 639 (https://en.wikipedia.org/wiki/ISO_639).
	@Column(name = "locale")
	private String locale;

	//The level of the place (ex. "nation", "province", "state", "county", "city", "township", "parish", "island", "archipelago",
	// "continent", "unincorporated town", "settlement", "village", "address").
	@Column(name = "type")
	private String type;

	//Ex. a latitude and longitude pair, or X and Y coordinates.
	@Column(name = "coordinate")
	private String coordinate;

	//The coordinate system (ex. "WGS84", "UTM").
	@Column(name = "coordinate_system")
	private String coordinateSystem;

	//A quantitative evaluation of the credibility of a piece of information, based upon its supporting evidence
	// ("unreliable/estimated data", "questionable reliability of evidence", "secondary evidence, data officially recorded sometime after
	// assertion", "direct and primary evidence used, or by dominance of the evidence").
	@Column(name = "coordinate_credibility")
	private String coordinateCredibility;

	//The primary photo for this place.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "photo_id", referencedColumnName = "id")
	private MediaEntity photo;

	//Top-left coordinate and width-height length of the enclosing box inside an photo.
	@Column(name = "photo_crop")
	private String photoCrop;


	public Long getID(){
		return id;
	}


	@Override
	public boolean equals(Object o){
		if(this == o)
			return true;
		if(!(o instanceof final PlaceEntity other))
			return false;
		return (id != null && id.equals(other.getID()));
	}

	@Override
	public int hashCode(){
		return 31;
	}

}
