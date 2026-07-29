package io.github.mtrevisan.familylegacy.v2.ui.binding;

import org.apache.commons.lang3.StringUtils;

import javax.swing.JTextArea;


public class BoundTextArea extends JTextArea implements PathBound{

	private String path;


	public BoundTextArea(final String path, final int rows, final int columns){
		super(rows, columns);

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
		setText(StringUtils.defaultString(value));
	}

	public boolean isEmpty(){
		return StringUtils.isEmpty(getValue());
	}

}
