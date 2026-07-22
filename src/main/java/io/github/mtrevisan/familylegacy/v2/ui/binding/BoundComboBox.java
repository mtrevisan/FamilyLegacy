package io.github.mtrevisan.familylegacy.v2.ui.binding;

import javax.swing.*;
import java.util.Objects;


public class BoundComboBox<E> extends JComboBox<E> implements PathBound{

	private String path;


	public BoundComboBox(final String path, final E[] items){
		super(items);

		this.path = path;
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
		final Object selectedItem = getSelectedItem();
		return (selectedItem != null? selectedItem.toString(): null);
	}

	@Override
	public void setValue(final String value){
		setSelectedItem(value);
	}

}
