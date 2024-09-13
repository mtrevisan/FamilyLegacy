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


@NodeEntity(label = "Media")
public class MediaEntity extends AbstractEntity{

	@Id
	@GeneratedValue
	private Long id;

	//An identifier for the media (must be unique, ex. a complete local or remote file reference (following RFC 1736 specifications) to the
	// auxiliary data).
	private String identifier;

	//The name of the media.
	private String title;

	private byte[] payload;

	//(ex. "photo", "audio", "video", "home movie", "newsreel", "microfilm", "microfiche", "cd-rom")
	private String type;

	//The projection/mapping/coordinate system of an photo. Known values include "spherical UV", "cylindrical equirectangular horizontal"/
	// "cylindrical equirectangular vertical" (equirectangular photo).
	private String photoProjection;

	//The date this media was first recorded.
	@Relationship(type = "on", direction = Relationship.Direction.INCOMING)
	private HistoricDateEntity date;


	@Override
	public Long getID(){
		return id;
	}

}
