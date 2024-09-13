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


//Transcriptions and transliterations of the name can be attached through a localized text (with type "name").
// Additional media can be attached.
@NodeEntity(label = "Place")
public class PlaceEntity extends AbstractEntity{

	@Id
	@GeneratedValue
	private Long id;

	//An identifier for the place (must be unique).
	private String identifier;

	//A verbatim copy of the name written in the original language.
	private String name;

	//Locale of the name as defined in ISO 639 (https://en.wikipedia.org/wiki/ISO_639).
	private String locale;

	//The level of the place (ex. "nation", "province", "state", "county", "city", "township", "parish", "island", "archipelago",
	// "continent", "unincorporated town", "settlement", "village", "address").
	private String type;

	//Ex. a latitude and longitude pair, or X and Y coordinates.
	private String coordinate;

	//The coordinate system (ex. "WGS84", "UTM").
	private String coordinateSystem;

	//A quantitative evaluation of the credibility of a piece of information, based upon its supporting evidence
	// ("unreliable/estimated data", "questionable reliability of evidence", "secondary evidence, data officially recorded sometime after
	// assertion", "direct and primary evidence used, or by dominance of the evidence").
	private String coordinateCredibility;

	//The primary photo for this place.
	@Relationship(type = "has", direction = Relationship.Direction.OUTGOING)
	private MediaEntity photo;

	//Top-left coordinate and width-height length of the enclosing box inside an photo.
	private String photoCrop;


	@Override
	public Long getID(){
		return id;
	}

}
