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


public class Source extends MediaContainer{

	private String id;
	private String auth;
	private String titl;
	private String abbr;
	private String publ;
	private String text;
	private RepositoryRef repo;
	private String refn;
	private String rin;
	private Change chan;
	private String medi;
	private String caln;
	private String _type;
	private String typeTag;
	private String _uid;
	private String uidTag;
	private String _paren;
	private String _italic;
	private String date;


	public String getId(){
		return id;
	}

	public void setId(final String id){
		this.id = id;
	}

	public String getAuthor(){
		return auth;
	}

	public void setAuthor(final String auth){
		this.auth = auth;
	}

	public String getTitle(){
		return titl;
	}

	public void setTitle(final String titl){
		this.titl = titl;
	}

	public String getAbbreviation(){
		return abbr;
	}

	public void setAbbreviation(final String abbr){
		this.abbr = abbr;
	}

	public String getPublicationFacts(){
		return publ;
	}

	public void setPublicationFacts(final String publ){
		this.publ = publ;
	}

	public String getText(){
		return text;
	}

	public void setText(final String text){
		this.text = text;
	}

	public RepositoryRef getRepositoryRef(){
		return repo;
	}

	public void setRepositoryRef(final RepositoryRef repo){
		this.repo = repo;
	}

	public Repository getRepository(final Gedcom gedcom){
		return (repo != null? repo.getRepository(gedcom): null);
	}

	public String getReferenceNumber(){
		return refn;
	}

	public void setReferenceNumber(final String refn){
		this.refn = refn;
	}

	public String getRin(){
		return rin;
	}

	public void setRin(final String rin){
		this.rin = rin;
	}

	public Change getChange(){
		return chan;
	}

	public void setChange(final Change chan){
		this.chan = chan;
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

	public String getUid(){
		return _uid;
	}

	public void setUid(final String _uid){
		this._uid = _uid;
	}

	public String getUidTag(){
		return uidTag;
	}

	public void setUidTag(final String uidTag){
		this.uidTag = uidTag;
	}

	public String getParen(){
		return _paren;
	}

	public void setParen(final String paren){
		this._paren = paren;
	}

	public String getItalic(){
		return _italic;
	}

	public void setItalic(final String italic){
		this._italic = italic;
	}

	public String getDate(){
		return date;
	}

	public void setDate(final String date){
		this.date = date;
	}

	//FIXME
//	public void accept(final Visitor visitor){
//		if(visitor.visit(this)){
//			if(repo != null)
//				repo.accept(visitor);
//			if(chan != null)
//				chan.accept(visitor);
//
//			super.visitContainedObjects(visitor);
//
//			visitor.endVisit(this);
//		}
//	}

}
