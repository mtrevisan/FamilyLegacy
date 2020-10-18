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

import java.util.Collections;
import java.util.Map;


//FIXME
public abstract class ExtensionContainer /*implements Visitable*/{

	private Extensions extensions;


	public Map<String, Object> getExtensions(){
		return (extensions != null? extensions.getExtensions(): Collections.emptyMap());
	}

	public Object getExtension(final String key){
		return (extensions != null? extensions.get(key): null);
	}

	public void setExtensions(final Map<String, Object> exts){
		if(exts != null && !exts.isEmpty()){
			if(extensions == null)
				extensions = new Extensions();

			extensions.setExtensions(exts);
		}
		else
			extensions = null;
	}

	public void putExtension(final String id, final Object extension){
		if(extensions == null)
			extensions = new Extensions();

		extensions.put(id, extension);
	}

	//FIXME
//	public void visitContainedObjects(final Visitor visitor){
//		for(final Map.Entry<String, Object> entry : getExtensions().entrySet())
//			visitor.visit(entry.getKey(), entry.getValue());
//	}

}
