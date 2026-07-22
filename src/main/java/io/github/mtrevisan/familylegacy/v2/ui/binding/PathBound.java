package io.github.mtrevisan.familylegacy.v2.ui.binding;


/**
 * Represents a UI component that can be bound to a specific path within a FLEF record.
 */
public interface PathBound{

	/**
	 * The dot‑separated path identifying where in the record this component reads/writes.
	 * May include zero‑based indices, e.g. {@code "NAME[1].VALUE"}.
	 */
	String getPath();

	/**
	 * Sets the path for this component.
	 */
	void setPath(String path);

	/**
	 * Returns the current value of the component (as its natural type).
	 */
	String getValue();

	/**
	 * Sets the component's value from the given value.
	 */
	void setValue(String value);

}
