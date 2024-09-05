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
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;


@Entity(name = "media")
@Table(uniqueConstraints = @UniqueConstraint(name = "unique_identifier", columnNames = "identifier"))
public class MediaEntity{

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "media_generator")
	@SequenceGenerator(name = "media_generator", allocationSize = 1, sequenceName = "media_sequence")
	@Column(name = "id")
	private Long id;

	//An identifier for the media (must be unique, ex. a complete local or remote file reference (following RFC 1736 specifications) to the
	// auxiliary data).
	@Column(name = "identifier", nullable = false)
	private String identifier;

	//The name of the media.
	@Column(name = "title")
	private String title;

	@Column(name = "payload", columnDefinition="BLOB")
	@Lob
	private byte[] payload;

	//(ex. "photo", "audio", "video", "home movie", "newsreel", "microfilm", "microfiche", "cd-rom")
	@Column(name = "type")
	private String type;

	//The projection/mapping/coordinate system of an photo. Known values include "spherical UV", "cylindrical equirectangular horizontal"/
	// "cylindrical equirectangular vertical" (equirectangular photo).
	@Column(name = "photo_projection")
	private String photoProjection;

	//The date this media was first recorded.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "date_id", referencedColumnName = "id")
	private HistoricDateEntity date;


	public Long getID(){
		return id;
	}


	@Override
	public boolean equals(Object o){
		if(this == o)
			return true;
		if(!(o instanceof final MediaEntity other))
			return false;
		return (id != null && id.equals(other.getID()));
	}

	@Override
	public int hashCode(){
		return 31;
	}

}
