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


public class Generator extends ExtensionContainer{

	private String value;
	private String name;
	private String vers;
	private GeneratorCorporation corp;
	private GeneratorData data;


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

	public String getVersion(){
		return vers;
	}

	public void setVersion(final String vers){
		this.vers = vers;
	}

	public GeneratorCorporation getGeneratorCorporation(){
		return corp;
	}

	public void setGeneratorCorporation(final GeneratorCorporation corp){
		this.corp = corp;
	}

	public GeneratorData getGeneratorData(){
		return data;
	}

	public void setGeneratorData(final GeneratorData data){
		this.data = data;
	}

	//FIXME
//	public void accept(final Visitor visitor){
//		if(visitor.visit(this)){
//			if(corp != null)
//				corp.accept(visitor);
//			if(data != null)
//				data.accept(visitor);
//
//			super.visitContainedObjects(visitor);
//
//			visitor.endVisit(this);
//		}
//	}

}
