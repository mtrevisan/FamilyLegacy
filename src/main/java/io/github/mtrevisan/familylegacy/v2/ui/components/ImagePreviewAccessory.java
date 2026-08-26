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
package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.ui.images.ScaledImage;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;


public class ImagePreviewAccessory extends JPanel implements PropertyChangeListener{

	private final ScaledImage previewImage = ScaledImage.createViewOnly();
	private SwingWorker<BufferedImage, Void> loaderWorker;

	private volatile File currentSelectedFile;


	public ImagePreviewAccessory(final JFileChooser chooser){
		chooser.addPropertyChangeListener(this);

		setLayout(new BorderLayout());

		setPreferredSize(new Dimension(250, 250));

		setBorder(BorderFactory.createTitledBorder("Preview"));

		add(previewImage, BorderLayout.CENTER);
	}


	@Override
	public void propertyChange(final PropertyChangeEvent evt){
		final String propertyName = evt.getPropertyName();

		if(JFileChooser.DIRECTORY_CHANGED_PROPERTY.equals(propertyName)){
			currentSelectedFile = null;

			if(loaderWorker != null && !loaderWorker.isDone())
				loaderWorker.cancel(true);

			previewImage.setRectangularImage(null);
		}

		else if(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY.equals(propertyName)){
			final File selectedFile = (File)evt.getNewValue();

			currentSelectedFile = selectedFile;

			// Cancel any ongoing image loading task
			if(loaderWorker != null && !loaderWorker.isDone())
				loaderWorker.cancel(true);

			// Clear previous preview while the new image is loading
			previewImage.setRectangularImage(null);

			if(selectedFile != null && selectedFile.isFile()){
				loaderWorker = new SwingWorker<>(){
					@Override
					protected BufferedImage doInBackground(){
						try{
							final BufferedImage image = ImageIO.read(selectedFile);
							if(image == null || isCancelled())
								return null;

							return image;
						}
						catch(final Exception e){
							return null;
						}
					}

					@Override
					protected void done(){
						if(isCancelled())
							return;

						// Ignore stale workers that completed after another file was already selected
						if(!selectedFile.equals(currentSelectedFile))
							return;

						try{
							previewImage.setRectangularImage(get());
						}
						catch(final Exception e){
							previewImage.setRectangularImage(null);
						}
					}
				};
				loaderWorker.execute();
			}
		}
	}

}
