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
package io.github.mtrevisan.familylegacy.v2.ui.binding;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;

import java.util.ArrayList;
import java.util.List;


/**
 * Manages a set of bound UI components and provides bulk load/save operations.
 */
public class BindingManager{

	private final List<PathBound> boundComponents = new ArrayList<>();


	/**
	 * Registers a bound component.
	 *
	 * @param component the component to register
	 * @return this manager (for fluent calls)
	 */
	public BindingManager bind(final PathBound component){
		if(component != null)
			boundComponents.add(component);

		return this;
	}

	/**
	 * Loads values into all registered components from the given record,
	 * using each component's path.
	 *
	 * @param record the record to read from
	 */
	public void load(final FLEFRecord record){
		for(final PathBound comp : boundComponents){
			final String path = comp.getPath();
			if(path == null || path.isEmpty())
				continue;

			final String value = FLEFRecordHelper.getChildValuesAsString(record, path);
			comp.setValue(value);
		}
	}

	/**
	 * Saves values from all registered components back to the given record,
	 * using each component's path.
	 *
	 * @param record the record to write into
	 */
	public void save(final FLEFRecord record){
		for(final PathBound comp : boundComponents){
			final String path = comp.getPath();
			if(path == null || path.isEmpty())
				continue;

			final String value = comp.getValue();
			if(value != null && !value.isEmpty())
				FLEFRecordHelper.updateChildValue(record, path, value);
		}
	}

	/**
	 * Removes all registered components.
	 */
	public void clear(){
		boundComponents.clear();
	}

}
