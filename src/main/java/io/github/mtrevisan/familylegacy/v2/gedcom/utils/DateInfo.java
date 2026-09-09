/**
 * Copyright (c) 2026 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.v2.gedcom.utils;


public class DateInfo{
	public enum Type{
		POINT,
		BOUNDED,
		SPANNING
	}

	private final Type type;
	private final String value;       // for POINT
	private final String notBefore;   // for BOUNDED
	private final String notAfter;    // for BOUNDED
	private final String from;        // for SPANNING
	private final String to;          // for SPANNING
	private final String qualifier;   // "ABT", "CAL", "EST", etc.
	private final boolean approximate;

	private DateInfo(Builder builder){
		this.type = builder.type;
		this.value = builder.value;
		this.notBefore = builder.notBefore;
		this.notAfter = builder.notAfter;
		this.from = builder.from;
		this.to = builder.to;
		this.qualifier = builder.qualifier;
		this.approximate = builder.approximate;
	}

	public Type getType(){
		return type;
	}

	public String getValue(){
		return value;
	}

	public String getNotBefore(){
		return notBefore;
	}

	public String getNotAfter(){
		return notAfter;
	}

	public String getFrom(){
		return from;
	}

	public String getTo(){
		return to;
	}

	public String getQualifier(){
		return qualifier;
	}

	public boolean isApproximate(){
		return approximate;
	}

	public static class Builder{
		private Type type = Type.POINT;
		private String value;
		private String notBefore;
		private String notAfter;
		private String from;
		private String to;
		private String qualifier;
		private boolean approximate = false;

		public Builder type(Type type){
			this.type = type;
			return this;
		}

		public Builder value(String value){
			this.value = value;
			return this;
		}

		public Builder notBefore(String notBefore){
			this.notBefore = notBefore;
			return this;
		}

		public Builder notAfter(String notAfter){
			this.notAfter = notAfter;
			return this;
		}

		public Builder from(String from){
			this.from = from;
			return this;
		}

		public Builder to(String to){
			this.to = to;
			return this;
		}

		public Builder qualifier(String qualifier){
			this.qualifier = qualifier;
			return this;
		}

		public Builder approximate(boolean approximate){
			this.approximate = approximate;
			return this;
		}

		public DateInfo build(){
			// Validation: at least one field must be set
			return new DateInfo(this);
		}
	}

}
