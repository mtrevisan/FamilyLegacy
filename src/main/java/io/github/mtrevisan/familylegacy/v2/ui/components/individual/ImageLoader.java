package io.github.mtrevisan.familylegacy.v2.ui.components.individual;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;


// TODO used?
/**
 * Loads individual images asynchronously with caching.
 */
public final class ImageLoader{

	private static final Map<String, ImageIcon> CACHE = new ConcurrentHashMap<>();
	private static final ImageIcon PLACEHOLDER_ICON;

	static{
		// Create a simple placeholder icon (gray square with text)
		BufferedImage img = new BufferedImage(48, 64, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = img.createGraphics();
		g2.setColor(Color.LIGHT_GRAY);
		g2.fillRect(0, 0, 48, 64);
		g2.setColor(Color.DARK_GRAY);
		g2.drawString("No img", 8, 30);
		g2.dispose();
		PLACEHOLDER_ICON = new ImageIcon(img);
	}

	private ImageLoader(){
	}

	/**
	 * Loads an image asynchronously, calling the callback on the EDT when done.
	 *
	 * @param uri      file path or URL
	 * @param crop     crop string "x y width height" or null
	 * @param maxSize  maximum dimension for the icon
	 * @param callback called on EDT with the loaded icon, or null on failure
	 */
	public static void loadImageAsync(String uri, String crop, Dimension maxSize,
		java.util.function.Consumer<ImageIcon> callback){
		if(uri == null){
			callback.accept(PLACEHOLDER_ICON);
			return;
		}

		// Check cache first
		String key = uri + (crop != null? "|" + crop: "");
		ImageIcon cached = CACHE.get(key);
		if(cached != null){
			SwingUtilities.invokeLater(() -> callback.accept(cached));
			return;
		}

		// Load in background
		new SwingWorker<ImageIcon, Void>(){
			@Override
			protected ImageIcon doInBackground(){
				try{
					File file = new File(uri);
					if(!file.exists()) return PLACEHOLDER_ICON;
					BufferedImage img = ImageIO.read(file);
					if(img == null) return PLACEHOLDER_ICON;

					// Apply crop if provided
					if(crop != null && !crop.isEmpty()){
						String[] parts = crop.split(" ");
						if(parts.length == 4){
							try{
								int x = Integer.parseInt(parts[0]);
								int y = Integer.parseInt(parts[1]);
								int w = Integer.parseInt(parts[2]);
								int h = Integer.parseInt(parts[3]);
								if(x >= 0 && y >= 0 && w > 0 && h > 0 &&
									x + w <= img.getWidth() && y + h <= img.getHeight()){
									img = img.getSubimage(x, y, w, h);
								}
							}
							catch(NumberFormatException ignored){
							}
						}
					}

					// Scale to fit within maxSize preserving aspect ratio
					int maxW = maxSize.width;
					int maxH = maxSize.height;
					double scale = Math.min((double)maxW / img.getWidth(), (double)maxH / img.getHeight());
					if(scale < 1.0){
						int newW = (int)(img.getWidth() * scale);
						int newH = (int)(img.getHeight() * scale);
						Image scaled = img.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
						ImageIcon icon = new ImageIcon(scaled);
						CACHE.put(key, icon);
						return icon;
					}
					else{
						ImageIcon icon = new ImageIcon(img);
						CACHE.put(key, icon);
						return icon;
					}
				}
				catch(IOException e){
					return PLACEHOLDER_ICON;
				}
			}

			@Override
			protected void done(){
				try{
					ImageIcon icon = get();
					callback.accept(icon != null? icon: PLACEHOLDER_ICON);
				}
				catch(Exception e){
					callback.accept(PLACEHOLDER_ICON);
				}
			}
		}.execute();
	}

}
