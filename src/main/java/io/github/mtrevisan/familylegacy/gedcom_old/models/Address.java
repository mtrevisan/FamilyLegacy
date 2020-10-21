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


public class Address extends ExtensionContainer{

	private String value;
	private String adr1;
	private String adr2;
	private String adr3;
	private String city;
	private String stae;
	private String post;
	private String ctry;
	private String _name;


	private void appendValue(final StringBuilder buf, final String value){
		if(value != null){
			if(buf.length() > 0)
				buf.append("\n");
			buf.append(value);
		}
	}

	public String getDisplayValue(){
		final StringBuilder sb = new StringBuilder();
		appendValue(sb, value);
		appendValue(sb, adr1);
		appendValue(sb, adr2);
		appendValue(sb, adr3);
		appendValue(sb, (city != null? city: "") + (city != null && stae != null? ", ": "") + (stae != null? stae: "")
			+ ((city != null || stae != null) && post != null? " ": "") + (post != null? post: ""));
		appendValue(sb, ctry);
		return sb.toString();
	}

	public String getValue(){
		return value;
	}

	public void setValue(final String value){
		this.value = value;
	}

	public String getAddressLine1(){
		return adr1;
	}

	public void setAddressLine1(final String adr1){
		this.adr1 = adr1;
	}

	public String getAddressLine2(){
		return adr2;
	}

	public void setAddressLine2(final String adr2){
		this.adr2 = adr2;
	}

	public String getAddressLine3(){
		return adr3;
	}

	public void setAddressLine3(final String adr3){
		this.adr3 = adr3;
	}

	public String getCity(){
		return city;
	}

	public void setCity(final String city){
		this.city = city;
	}

	public String getState(){
		return stae;
	}

	public void setState(final String stae){
		this.stae = stae;
	}

	public String getPostalCode(){
		return post;
	}

	public void setPostalCode(final String post){
		this.post = post;
	}

	public String getCountry(){
		return ctry;
	}

	public void setCountry(final String ctry){
		this.ctry = ctry;
	}

	public String getName(){
		return _name;
	}

	public void setName(final String name){
		_name = name;
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
