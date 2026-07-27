package io.github.mtrevisan.familylegacy.v2.ui.binding;

import org.apache.commons.lang3.StringUtils;

import javax.swing.JTextField;


public class BoundTextField extends JTextField implements PathBound{

	private String path;


	public BoundTextField(final String path, int columns){
		super(columns);
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
		return getText();
	}

	@Override
	public void setValue(final String value){
		setText(value != null? value: StringUtils.EMPTY);
	}

	public boolean isEmpty(){
		final String value = getValue();
		return (value == null || value.trim().isEmpty());
	}

}
