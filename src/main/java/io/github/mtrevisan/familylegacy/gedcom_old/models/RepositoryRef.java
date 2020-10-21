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
package io.github.mtrevisan.familylegacy.gedcom_old.models;


public class RepositoryRef extends NoteContainer{

	private final String ref;
	private String value;
	private String caln;
	private String medi;
	//use string instead of boolean so it isn't saved to json when false
	private String isMediUnderCalnTag;


	public RepositoryRef(final String ref){
		this.ref = ref;
	}

	public String getRef(){
		return ref;
	}

	public String getValue(){
		return value;
	}

	public void setValue(final String value){
		this.value = value;
	}

	public Repository getRepository(final Gedcom gedcom){
		return gedcom.getRepository(ref);
	}

	public String getCallNumber(){
		return caln;
	}

	public void setCallNumber(final String caln){
		this.caln = caln;
	}

	public String getMediaType(){
		return medi;
	}

	public void setMediaType(final String medi){
		this.medi = medi;
	}

	public boolean isMediUnderCalnTag(){
		return isMediUnderCalnTag != null;
	}

	public void setMediUnderCalnTag(final boolean mediUnderCalnTag){
		isMediUnderCalnTag = (mediUnderCalnTag? "true": null);
	}

	//FIXME
//	public void accept(final Visitor visitor){
//		if(visitor.visit(this)){
//			super.visitContainedObjects(visitor);
//
//			visitor.endVisit(this);
//		}
//	}

}
