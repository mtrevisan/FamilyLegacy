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


public class SpouseRef extends ExtensionContainer{

	private String ref;
	private String _pref;


	public String getRef(){
		return ref;
	}

	public void setRef(final String ref){
		this.ref = ref;
	}

	/**
	 * Convenience function to dereference person.
	 *
	 * @param gedcom	Gedcom
	 * @return	Referenced person
	 */
	public Person getPerson(final Gedcom gedcom){
		return gedcom.getPerson(ref);
	}

	public String getPreferred(){
		return _pref;
	}

	public void setPreferred(final String pref){
		this._pref = pref;
	}

	//FIXME
//	public void accept(final Visitor visitor){
//		throw new RuntimeException("Not implemented - pass isHusband");
//	}

	//FIXME
	/**
	 * Handle the visitor
	 *
	 * @param visitor	Visitor
	 * @param isHusband	{@code false} for wife; ChildRef overrides this method
	 */
//	public void accept(final Visitor visitor, final boolean isHusband){
//		if(visitor.visit(this, isHusband)){
//			super.visitContainedObjects(visitor);
//
//			visitor.endVisit(this);
//		}
//	}

}
