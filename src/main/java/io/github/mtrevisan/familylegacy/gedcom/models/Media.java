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


public class Media extends NoteContainer{

	private String id;
	private String form;
	private String title;
	private String blob;
	private Change change;
	private String _file;
	private String fileTag;
	private String _primary;
	private String _type;
	private String _scbk;
	private String _sshow;


	public String getId(){
		return id;
	}

	public void setId(final String id){
		this.id = id;
	}

	public String getFormat(){
		return form;
	}

	public void setFormat(final String form){
		this.form = form;
	}

	public String getTitle(){
		return title;
	}

	public void setTitle(final String title){
		this.title = title;
	}

	public String getBlob(){
		return blob;
	}

	public void setBlob(final String blob){
		this.blob = blob;
	}

	public Change getChange(){
		return change;
	}

	public void setChange(final Change change){
		this.change = change;
	}

	public String getFile(){
		return _file;
	}

	public void setFile(final String _file){
		this._file = _file;
	}

	public String getFileTag(){
		return fileTag;
	}

	public void setFileTag(final String fileTag){
		this.fileTag = fileTag;
	}

	public String getPrimary(){
		return _primary;
	}

	public void setPrimary(final String primary){
		this._primary = primary;
	}

	public String getType(){
		return _type;
	}

	public void setType(final String type){
		this._type = type;
	}

	public String getScrapbook(){
		return _scbk;
	}

	public void setScrapbook(final String scbk){
		this._scbk = scbk;
	}

	public String getSlideShow(){
		return _sshow;
	}

	public void setSlideShow(final String sshow){
		this._sshow = sshow;
	}

	//FIXME
//	public void accept(final Visitor visitor){
//		if(visitor.visit(this)){
//			if(change != null)
//				change.accept(visitor);
//
//			super.visitContainedObjects(visitor);
//
//			visitor.endVisit(this);
//		}
//	}

}
