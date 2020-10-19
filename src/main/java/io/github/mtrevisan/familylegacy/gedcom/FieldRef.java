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
package io.github.mtrevisan.familylegacy.gedcom;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


public class FieldRef{

	private final Object target;
	private final String name;


	public FieldRef(final Object target, final String name){
		this.target = target;
		this.name = name;
	}

	public String getClassFieldName(){
		return target.getClass().getName() + "." + name;
	}

	public Object getTarget(){
		return target;
	}

	public String getFieldName(){
		return name;
	}

	public void setValue(final String value) throws NoSuchMethodException{
		try{
			final Method method = target.getClass().getMethod("set" + name, String.class);
			method.invoke(target, value);
		}
		catch(final InvocationTargetException | IllegalAccessException e){
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public String getValue() throws NoSuchMethodException{
		try{
			final Method method = target.getClass().getMethod("get" + name);
			return (String) method.invoke(target);
		}
		catch(final InvocationTargetException | IllegalAccessException e){
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public void appendValue(final String value) throws NoSuchMethodException{
		try{
			final String currentValue = getValue();
			setValue((currentValue == null? "": currentValue) + value);
		}
		catch(final NoSuchMethodException e){
			//try "add"
			try{
				final Method method = target.getClass().getMethod("add" + name, String.class);
				method.invoke(target, value);
			}
			catch(final InvocationTargetException | IllegalAccessException e1){
				e1.printStackTrace();
				throw new RuntimeException(e);
			}
		}
	}

}
