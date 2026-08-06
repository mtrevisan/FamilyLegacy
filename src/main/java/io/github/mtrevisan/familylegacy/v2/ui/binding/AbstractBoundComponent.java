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

import javax.swing.JComponent;
import java.util.function.BiConsumer;
import java.util.function.Function;


/**
 * Abstract base for Swing components that are bound to a record path.
 * Subclasses must provide a way to convert between the component's type and {@code String}.
 *
 * @param <C>	The concrete Swing component type (e.g., {@code JTextField}, {@code JComboBox})
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
