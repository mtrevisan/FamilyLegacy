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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;


/**
 * Make Extensions a class so we can use an ExtensionsTypeAdapter with Gson
 */
public class Extensions{

	private Map<String, Object> extensions = new HashMap<>(0);


	public Extensions(){}

	public Map<String, Object> getExtensions(){
		return extensions;
	}

	public void setExtensions(final Map<String, Object> extensions){
		this.extensions = extensions;
	}

	public Object get(final String key){
		return extensions.get(key);
	}

	public void put(final String key, final Object extension){
		extensions.put(key, extension);
	}

	public Set<String> getKeys(){
		return extensions.keySet();
	}

}
