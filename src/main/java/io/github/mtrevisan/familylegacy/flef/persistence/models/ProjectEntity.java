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
import jakarta.persistence.Id;

import java.time.ZonedDateTime;


@Entity(name = "project")
public class ProjectEntity extends AbstractEntity{

	@Id
	@Column(name = "id", nullable = false)
	private Long id;

	//"Family LEgacy Format"
	@Column(name = "protocol_name", nullable = false)
	private String protocolName;

	//"0.0.10"
	@Column(name = "protocol_version", nullable = false)
	private String protocolVersion;

	//A copyright statement.
	@Column(name = "copyright")
	private String copyright;

	//Text following markdown language.
	@Column(name = "note")
	private String note;

	//Locale of the name as defined in ISO 639 (https://en.wikipedia.org/wiki/ISO_639).
	@Column(name = "locale")
	private String locale;

	@Column(name = "creation_date", columnDefinition= "TIMESTAMP WITH TIME ZONE", nullable = false)
	private ZonedDateTime creationDate;

	@Column(name = "update_date", columnDefinition= "TIMESTAMP WITH TIME ZONE")
	private ZonedDateTime updateDate;


	@Override
	public Long getID(){
		return id;
	}

}
