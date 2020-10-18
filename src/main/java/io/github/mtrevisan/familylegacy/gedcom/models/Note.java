/**
 * Copyright (c) 2020 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.gedcom.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * NOTE: don't make this a SourceCitationContainer, because that would allow nested notes
 */
public class Note extends ExtensionContainer{

	private String id;
	private String value;
	private String rin;
	private Change change;
	private List<SourceCitation> sourceCitations;
	//yuck: Reunion does this: 0 NOTE 1 CONT ... 2 SOUR; remember for round-trip
	private boolean sourceCitationsUnderValue;


	public String getId(){
		return id;
	}

	public void setId(final String id){
		this.id = id;
	}

	public String getValue(){
		return value;
	}

	public void setValue(final String value){
		this.value = value;
	}

	public String getRin(){
		return rin;
	}

	public void setRin(final String rin){
		this.rin = rin;
	}

	public Change getChange(){
		return change;
	}

	public void setChange(final Change change){
		this.change = change;
	}

	public List<SourceCitation> getSourceCitations(){
		return (sourceCitations != null? sourceCitations: Collections.emptyList());
	}

	public void setSourceCitations(final List<SourceCitation> sourceCitations){
		this.sourceCitations = sourceCitations;
	}

	public void addSourceCitation(final SourceCitation sourceCitation){
		if(sourceCitations == null)
			sourceCitations = new ArrayList<>(1);

		sourceCitations.add(sourceCitation);
	}

	public boolean isSourceCitationsUnderValue(){
		return sourceCitationsUnderValue;
	}

	public void setSourceCitationsUnderValue(final boolean sourceRefsUnderValue){
		this.sourceCitationsUnderValue = sourceRefsUnderValue;
	}

	//FI§XME
//	public void visitContainedObjects(final Visitor visitor, final boolean includeSourceCitations){
//		if(change != null)
//			change.accept(visitor);
//
//		if(includeSourceCitations)
//			for(final SourceCitation sourceCitation : getSourceCitations())
//				sourceCitation.accept(visitor);
//
//		super.visitContainedObjects(visitor);
//	}
//
//	public void visitContainedObjects(final Visitor visitor){
//		visitContainedObjects(visitor, true);
//	}
//
//	public void accept(final Visitor visitor){
//		if(visitor.visit(this)){
//			this.visitContainedObjects(visitor);
//
//			visitor.endVisit(this);
//		}
//	}

}
