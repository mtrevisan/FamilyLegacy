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

import java.util.StringJoiner;


public class Name extends SourceCitationContainer{

	private String value;
	private String givn;
	private String surn;
	private String npfx;
	private String nsfx;
	private String spfx;
	private String nick;
	private String fone;
	private String romn;
	private String _type;
	private String typeTag;
	private String _aka;
	private String akaTag;
	private String foneTag;
	private String romnTag;
	private String _marrnm;
	private String marrnmTag;


	public String getDisplayValue(){
		if(value != null)
			return value;

		return new StringJoiner(" ")
			.add(npfx)
			.add(givn)
			.add(spfx)
			.add(surn)
			.add(nsfx)
			.toString().trim();
	}

	public String getValue(){
		return value;
	}

	public void setValue(final String value){
		this.value = value;
	}

	public String getGiven(){
		return givn;
	}

	public void setGiven(final String givn){
		this.givn = givn;
	}

	public String getSurname(){
		return surn;
	}

	public void setSurname(final String surn){
		this.surn = surn;
	}

	public String getPrefix(){
		return npfx;
	}

	public void setPrefix(final String npfx){
		this.npfx = npfx;
	}

	public String getSuffix(){
		return nsfx;
	}

	public void setSuffix(final String nsfx){
		this.nsfx = nsfx;
	}

	public String getSurnamePrefix(){
		return spfx;
	}

	public void setSurnamePrefix(final String spfx){
		this.spfx = spfx;
	}

	public String getNickname(){
		return nick;
	}

	public void setNickname(final String nick){
		this.nick = nick;
	}

	/**
	 * Name has a type of ALIA when the GEDCOM had a ALIA sub-tag of INDI.
	 *
	 * @return	The type.
	 */
	public String getType(){
		return _type;
	}

	public void setType(final String _type){
		this._type = _type;
	}

	public String getTypeTag(){
		return typeTag;
	}

	public void setTypeTag(final String typeTag){
		this.typeTag = typeTag;
	}

	public String getAka(){
		return _aka;
	}

	public void setAka(final String _aka){
		this._aka = _aka;
	}

	public String getAkaTag(){
		return akaTag;
	}

	public void setAkaTag(final String akaTag){
		this.akaTag = akaTag;
	}

	public String getRomn(){
		return romn;
	}

	public void setRomn(final String romn){
		this.romn = romn;
	}

	public String getRomnTag(){
		return romnTag;
	}

	public void setRomnTag(final String romnTag){
		this.romnTag = romnTag;
	}

	public String getFone(){
		return fone;
	}

	public void setFone(final String fone){
		this.fone = fone;
	}

	public String getFoneTag(){
		return foneTag;
	}

	public void setFoneTag(final String foneTag){
		this.foneTag = foneTag;
	}

	public String getMarriedName(){
		return _marrnm;
	}

	public void setMarriedName(final String _marrnm){
		this._marrnm = _marrnm;
	}

	public String getMarriedNameTag(){
		return marrnmTag;
	}

	public void setMarriedNameTag(final String marrnmTag){
		this.marrnmTag = marrnmTag;
	}

	//FIXME
//	public void accept(final Visitor visitor){
//		if(visitor.visit(this)){
//			super.visitContainedObjects(visitor);
//			visitor.endVisit(this);
//		}
//	}

}
