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


/* DONE */
public class ImagePreviewAccessory extends JPanel implements PropertyChangeListener{

	private final ScaledImage previewImage = ScaledImage.createViewOnly();
	private SwingWorker<BufferedImage, Void> loaderWorker;


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
		if(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY.equals(propertyName)){
			final File selectedFile = (File)evt.getNewValue();

			// Cancel any ongoing image loading task
			if(loaderWorker != null && !loaderWorker.isDone())
				loaderWorker.cancel(true);

			if(selectedFile != null && selectedFile.isFile()){
				loaderWorker = new SwingWorker<>(){
					@Override
					protected BufferedImage doInBackground(){
						try{
							return ImageIO.read(selectedFile);
						}
						catch(final Exception e){
							return null;
						}
					}

					@Override
					protected void done(){
						if(!isCancelled()){
							try{
								previewImage.setRectangularImage(get());
							}
							catch(final Exception e){
								previewImage.setRectangularImage(null);
							}
						}
					}
				};
				loaderWorker.execute();
			}
			else
				previewImage.setRectangularImage(null);
		}
	}

}
