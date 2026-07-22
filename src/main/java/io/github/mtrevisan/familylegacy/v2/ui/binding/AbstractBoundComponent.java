package io.github.mtrevisan.familylegacy.v2.ui.binding;

import javax.swing.*;
import java.util.function.BiConsumer;
import java.util.function.Function;


/**
 * Abstract base for Swing components that are bound to a record path.
 * Subclasses must provide a way to convert between the component's type and String.
 *
 * @param <C> the concrete Swing component type (e.g., JTextField, JComboBox)
 */
public abstract class AbstractBoundComponent<C extends JComponent> implements PathBound{

	protected final C component;
	protected String path;

	/** Converter from component state to value. */
	protected final Function<C, String> toValue;
	/** Converter from value to component state. */
	protected final BiConsumer<C, String> toComponent;


	protected AbstractBoundComponent(final C component, final String path, final Function<C, String> toValue,
			final BiConsumer<C, String> toComponent){
		this.component = component;
		this.path = path;
		this.toValue = toValue;
		this.toComponent = toComponent;
	}

	@Override
	public String getPath(){
		return path;
	}

	@Override
	public void setPath(final String path){
		this.path = path;
	}

	@Override
	public String getValue(){
		return toValue.apply(component);
	}

	@Override
	public void setValue(final String value){
		toComponent.accept(component, value);
	}

	/**
	 * Convenience method to get the underlying Swing component for layout.
	 */
	public C getComponent(){
		return component;
	}

}
