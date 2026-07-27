package io.github.mtrevisan.familylegacy.v2.ui.binding;

import javax.swing.JCheckBox;


public class BoundCheckBox extends JCheckBox implements PathBound{

	private String path;


	public BoundCheckBox(final String path, final String text){
		super(text);

		this.path = path;
	}


	@Override
	public String getPath(){
		return path;
	}

	@Override
	public void setPath(String path){
		this.path = path;
	}

	@Override
	public String getValue(){
		return Boolean.toString(isSelected());
	}

	@Override
	public void setValue(String value){
		setSelected(Boolean.parseBoolean(value));
	}

}
