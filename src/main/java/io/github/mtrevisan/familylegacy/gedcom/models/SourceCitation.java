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


public class SourceCitation extends MediaContainer{

	public enum DataTagContents{DATE, TEXT, COMBINED, SEPARATE}

	private final String ref;
	private String value;
	private String page;
	private String date;
	private String text;
	private String quay;
	//yuck - some gedcom's don't use the data tag, some include write both text and date under the same tag, others use two data tags
	//set to null in default case (no data tag) so it isn't saved to json
	private DataTagContents dataTagContents;


	public SourceCitation(final String ref){
		this.ref = ref;
	}

	public String getRef(){
		return ref;
	}

	public Source getSource(final Gedcom gedcom){
		return gedcom.getSource(ref);
	}

	/**
	 * Use this function to get text from value or text field
	 *
	 * @return	Text, or value if text is empty
	 */
	public String getTextOrValue(){
		return (text != null? text: value);
	}

	public String getValue(){
		return value;
	}

	public void setValue(final String value){
		this.value = value;
	}

	public String getText(){
		return text;
	}

	public void setText(final String text){
		this.text = text;
	}

	public String getPage(){
		return page;
	}

	public void setPage(final String page){
		this.page = page;
	}

	public String getDate(){
		return date;
	}

	public void setDate(final String date){
		this.date = date;
	}

	public String getQuality(){
		return quay;
	}

	public void setQuality(final String quay){
		this.quay = quay;
	}

	public DataTagContents getDataTagContents(){
		return dataTagContents;
	}

	public void setDataTagContents(final DataTagContents dataTagContents){
		this.dataTagContents = dataTagContents;
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
