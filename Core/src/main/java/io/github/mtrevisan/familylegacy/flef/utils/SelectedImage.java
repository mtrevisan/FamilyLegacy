package io.github.mtrevisan.familylegacy.flef.utils;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;


public final class SelectedImage{

	private final String resourceUri;
	private final BufferedImage image;
	private final Rectangle crop;


	public SelectedImage(
		final String resourceUri,
		final BufferedImage image,
		final Rectangle crop){

		this.resourceUri = resourceUri;
		this.image = image;
		this.crop = crop;
	}


	public String getResourceUri(){
		return resourceUri;
	}

	public BufferedImage getImage(){
		return image;
	}

	public Rectangle getCrop(){
		return crop;
	}

}

