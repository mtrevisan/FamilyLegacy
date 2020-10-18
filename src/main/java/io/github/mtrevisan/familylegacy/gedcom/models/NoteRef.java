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
 * NOTE: don't make this a SourceCitationContainer, because that would allow notes within note refs
 */
public class NoteRef extends ExtensionContainer{

	private String ref;
	private List<SourceCitation> sourceCitations;


	public String getRef(){
		return ref;
	}

	public void setRef(final String ref){
		this.ref = ref;
	}

	/**
	 * Convenience function to dereference note.
	 *
	 * @param gedcom	Gedcom
	 * @return	Referenced note
	 */
	public Note getNote(final Gedcom gedcom){
		return gedcom.getNote(ref);
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

	//FIXME
//	public void accept(final Visitor visitor){
//		if(visitor.visit(this)){
//			for(final SourceCitation sourceCitation : getSourceCitations())
//				sourceCitation.accept(visitor);
//
//			super.visitContainedObjects(visitor);
//
//			visitor.endVisit(this);
//		}
//	}

}
