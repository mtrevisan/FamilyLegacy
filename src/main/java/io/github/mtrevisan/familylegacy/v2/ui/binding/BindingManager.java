package io.github.mtrevisan.familylegacy.v2.ui.binding;

import io.github.mtrevisan.familylegacy.v2.io.FLEFFile;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;

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

			final String value = FLEFFile.getValueByPath(record, path);
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
				FLEFFile.setValueByPath(record, path, value);
		}
	}

	/**
	 * Removes all registered components.
	 */
	public void clear(){
		boundComponents.clear();
	}

}
