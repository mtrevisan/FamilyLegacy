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


//Where a source makes an assertion.
@NodeEntity(label = "Citation")
public class CitationEntity extends AbstractEntity{

	@Id
	@GeneratedValue
	private Long id;

	//The source from which this citation is extracted.
	@Relationship(type = "has", direction = Relationship.Direction.INCOMING)
	private SourceEntity source;

	//The location of the citation inside the source (ex. "page 27, number 8, row 2").
	private String location;

	//A verbatim copy of any description contained within the source. Text following markdown language. Reference to an entry in a table can
	// be written as `[text](<TABLE_NAME>@<XREF>)`.
	private String extract;

	//Locale as defined in ISO 639 (https://en.wikipedia.org/wiki/ISO_639).
	private String extractLocale;

	//Can be 'transcript' (indicates a complete, verbatim copy of the document), 'extract' (a verbatim copy of part of the document), or
	// 'abstract' (a reworded summarization of the document content).
	private String extractType;


	@Override
		public Long getID(){
		return id;
	}

}
