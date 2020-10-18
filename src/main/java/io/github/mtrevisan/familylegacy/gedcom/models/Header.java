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


public class Header extends NoteContainer{

	private Generator sour;
	private String dest;
	private DateTime date;
	private String submRef;
	private String subnRef;
	private Submission subn;
	private String file;
	private String copr;
	private GedcomVersion gedc;
	private CharacterSet charset;
	private String lang;


	public Generator getGenerator(){
		return sour;
	}

	public void setGenerator(final Generator sour){
		this.sour = sour;
	}

	public String getDestination(){
		return dest;
	}

	public void setDestination(final String dest){
		this.dest = dest;
	}

	public DateTime getDateTime(){
		return date;
	}

	public void setDateTime(final DateTime date){
		this.date = date;
	}

	public String getSubmitterRef(){
		return submRef;
	}

	public void setSubmitterRef(final String submRef){
		this.submRef = submRef;
	}

	public Submitter getSubmitter(final Gedcom gedcom){
		return gedcom.getSubmitter(submRef);
	}

	/**
	 * Use Gedcom.getSubmission in place of this function
	 *
	 * @return submission reference
	 */
	public String getSubmissionRef(){
		return subnRef;
	}

	public void setSubmissionRef(final String subnRef){
		this.subnRef = subnRef;
	}

	/**
	 * Use Gedcom.getSubmission in place of this function
	 *
	 * @return submission
	 */
	Submission getSubmission(){
		return subn;
	}

	public void setSubmission(final Submission subn){
		this.subn = subn;
	}

	public String getFile(){
		return file;
	}

	public void setFile(final String file){
		this.file = file;
	}

	public String getCopyright(){
		return copr;
	}

	public void setCopyright(final String copr){
		this.copr = copr;
	}

	public GedcomVersion getGedcomVersion(){
		return gedc;
	}

	public void setGedcomVersion(final GedcomVersion gedc){
		this.gedc = gedc;
	}

	public CharacterSet getCharacterSet(){
		return charset;
	}

	public void setCharacterSet(final CharacterSet charset){
		this.charset = charset;
	}

	public String getLanguage(){
		return lang;
	}

	public void setLanguage(final String lang){
		this.lang = lang;
	}

	//FIXME
//	public void accept(final Visitor visitor){
//		if(visitor.visit(this)){
//			if(sour != null)
//				sour.accept(visitor);
//			if(date != null)
//				date.accept(visitor);
//			if(subn != null)
//				subn.accept(visitor);
//			if(gedc != null)
//				gedc.accept(visitor);
//			if(charset != null)
//				charset.accept(visitor);
//
//			super.visitContainedObjects(visitor);
//
//			visitor.endVisit(this);
//		}
//	}

}
