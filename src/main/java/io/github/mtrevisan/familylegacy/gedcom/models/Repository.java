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


public class Repository extends NoteContainer{

	private String id;
	private String value;
	private String name;
	private Address addr;
	private String phone;
	private String fax;
	private String rin;
	private Change change;
	private String _email;
	private String emailTag;
	private String _www;
	private String wwwTag;


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

	public String getName(){
		return name;
	}

	public void setName(final String name){
		this.name = name;
	}

	public Address getAddress(){
		return addr;
	}

	public void setAddress(final Address addr){
		this.addr = addr;
	}

	public String getPhone(){
		return phone;
	}

	public void setPhone(final String phone){
		this.phone = phone;
	}

	public String getFax(){
		return fax;
	}

	public void setFax(final String fax){
		this.fax = fax;
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

	public String getEmail(){
		return _email;
	}

	public void setEmail(final String email){
		this._email = email;
	}

	public String getEmailTag(){
		return emailTag;
	}

	public void setEmailTag(final String emailTag){
		this.emailTag = emailTag;
	}

	public String getWww(){
		return _www;
	}

	public void setWww(final String www){
		this._www = www;
	}

	public String getWwwTag(){
		return wwwTag;
	}

	public void setWwwTag(final String wwwTag){
		this.wwwTag = wwwTag;
	}

	//FIXME
//	public void accept(final Visitor visitor){
//		if(visitor.visit(this)){
//			if(addr != null)
//				addr.accept(visitor);
//			if(change != null)
//				change.accept(visitor);
//
//			super.visitContainedObjects(visitor);
//
//			visitor.endVisit(this);
//		}
//	}

}
