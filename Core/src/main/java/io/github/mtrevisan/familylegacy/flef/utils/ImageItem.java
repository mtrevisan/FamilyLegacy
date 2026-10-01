package io.github.mtrevisan.familylegacy.flef.utils;

import java.awt.image.BufferedImage;


public final class ImageItem{

	private final String displayName;
	private final String resourceUri;
	private final BufferedImage image;


	public ImageItem(
		final String displayName,
		final String resourceUri,
		final BufferedImage image){

		this.displayName = displayName;
		this.resourceUri = resourceUri;
		this.image = image;
	}


	public String getDisplayName(){
		return displayName;
	}

	public String getResourceUri(){
		return resourceUri;
	}

	public BufferedImage getImage(){
		return image;
	}

	@Override
	public String toString(){
		return displayName;
	}

}
