/**
 * Copyright (c) 2020 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.ui.utilities;

import io.github.mtrevisan.familylegacy.services.ResourceHelper;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;


public class ImagePreview extends JComponent implements PropertyChangeListener{

	private static final int WIDTH_PAD = 10;

	private ImageIcon thumbnail;
	private File file;


	public ImagePreview(final JFileChooser fileChooser, int preferredWidth, final int preferredHeight){
		preferredWidth = Math.max(preferredWidth, WIDTH_PAD * 3);
		setPreferredSize(new Dimension(preferredWidth, preferredHeight));

		fileChooser.addPropertyChangeListener(this);
	}

	public void loadImage() throws IOException{
		if(file == null)
			thumbnail = null;
		else{
			final BufferedImage image = ResourceHelper.readImage(file);

			final ImageIcon tmpIcon = new ImageIcon(image);
			final int maxWidth = getPreferredSize().width - WIDTH_PAD * 2;
			if(tmpIcon.getIconWidth() > maxWidth)
				thumbnail = new ImageIcon(tmpIcon.getImage().getScaledInstance(maxWidth, -1, Image.SCALE_DEFAULT));
			else
				//no need to miniaturize
				thumbnail = tmpIcon;
		}
	}

	@Override
	public void propertyChange(final PropertyChangeEvent evt){
		boolean update = true;

		//ff the directory changed, don't show an image
		final String property = evt.getPropertyName();
		if(JFileChooser.DIRECTORY_CHANGED_PROPERTY.equals(property))
			file = null;
		//if a file became selected, find out which one
		else if(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY.equals(property))
			file = (File)evt.getNewValue();
		else
			update = false;

		//update the preview accordingly
		if(update){
			thumbnail = null;
			if(isShowing()){
				try{
					loadImage();

					repaint();
				}
				catch(final IOException ignored){}
			}
		}
	}

	protected void paintComponent(final Graphics g){
		if(thumbnail == null){
			try{
				loadImage();
			}
			catch(final IOException ignored){}
		}

		if(thumbnail != null){
			//center image
			final int x = Math.max((getWidth() - thumbnail.getIconWidth()) / 2, WIDTH_PAD);
			final int y = Math.max((getHeight() - thumbnail.getIconHeight()) / 2, 0);

			thumbnail.paintIcon(this, g, x, y);
		}
	}

}
