/**
 * Copyright (c) 2021 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.flef.helpers;

import io.github.mtrevisan.familylegacy.flef.ui.dialogs.PhotoCropDialog;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.filechooser.FileSystemView;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;


public final class FileHelper{

	private static final Logger LOGGER = LoggerFactory.getLogger(FileHelper.class);


	private static final String URL_CONNECTIVITY_TEST = "https://www.google.com/";

	private static final Tika TIKA = new Tika();


	private FileHelper(){}


	public static Path documentsDirectory(){
		final FileSystemView fileSystemView = FileSystemView.getFileSystemView();
		return fileSystemView.getDefaultDirectory()
			.toPath();
	}


	public static File loadFile(final String filename){
		if(filename == null)
			return null;

		File file = new File(filename);
		if(!file.exists()){
			//try loading from resources
			final URL fileURL = PhotoCropDialog.class.getResource(filename);
			if(fileURL != null)
				file = new File(fileURL.getPath());
		}
		return file;
	}

	public static boolean isPhoto(final File file){
		try{
			final String mimeType = TIKA.detect(file);
			return (mimeType != null && mimeType.startsWith("image/"));
		}
		catch(final IOException ignored){
			return false;
		}
	}


	//https://stackoverflow.com/questions/18004150/desktop-api-is-not-supported-on-the-current-platform
	@SuppressWarnings("UseOfProcessBuilder")
	public static boolean browse(File file) throws IOException, InterruptedException{
		if(file.isFile())
			file = file.getParentFile();

		//try using Desktop first
		if(executeDesktopCommand(Desktop.Action.OPEN, file))
			return true;

		//backup to system-specific
		ProcessBuilder builder = null;
		final String absolutePath = file.getAbsolutePath();
		if(SystemUtils.IS_OS_WINDOWS)
			builder = new ProcessBuilder("explorer", absolutePath);
		else if(SystemUtils.IS_OS_LINUX){
			if(runOSCommand(new ProcessBuilder("kde-open", absolutePath))
				|| runOSCommand(new ProcessBuilder("gnome-open", absolutePath))
				|| runOSCommand(new ProcessBuilder("xdg-open", absolutePath))
			)
				return true;
		}
		else if(SystemUtils.IS_OS_MAC)
			builder = new ProcessBuilder("open", absolutePath);
		else
			LOGGER.warn("Cannot issue command to open file {}, OS not recognized ({})", file.getName(), SystemUtils.OS_NAME);

		return (builder != null && runOSCommand(builder));
	}

	public static boolean browseURL(final String url){
		return executeDesktopCommand(Desktop.Action.BROWSE, url);
	}

	//https://stackoverflow.com/questions/526037/how-to-open-user-system-preferred-editor-for-given-file
	@SuppressWarnings("UseOfProcessBuilder")
	public static boolean openFileWithChosenEditor(final File file) throws IOException, InterruptedException{
		//system-specific
		ProcessBuilder builder = null;
		if(SystemUtils.IS_OS_WINDOWS)
			builder = new ProcessBuilder("rundll32.exe", "shell32.dll,OpenAs_RunDLL", file.getAbsolutePath());
		else if(SystemUtils.IS_OS_LINUX)
			builder = new ProcessBuilder("edit", file.getAbsolutePath());
		else if(SystemUtils.IS_OS_MAC)
			builder = new ProcessBuilder("open", file.getAbsolutePath());
		else
			LOGGER.warn("Cannot issue command to open file {}, OS not recognized ({})", file.getName(), SystemUtils.OS_NAME);

		return (builder != null && runOSCommand(builder));
	}

	@SuppressWarnings("ConstantConditions")
	private static boolean executeDesktopCommand(final Desktop.Action action, final Object parameter){
		boolean done = false;
		final Desktop desktop = getDesktopFor(action);
		try{
			switch(action){
				case OPEN:
					desktop.open((File)parameter);
					done = true;
					break;

				case BROWSE:
					if(hasInternetConnectivity()){
						desktop.browse(new URI((String)parameter));
						done = true;
					}
					break;

				case MAIL:
					if(hasInternetConnectivity()){
						desktop.mail(new URI((String)parameter));
						done = true;
					}
			}
		}
		catch(final IOException | URISyntaxException e){
			LOGGER.error("Cannot execute {} command", action, e);
		}
		return done;
	}

	private static Desktop getDesktopFor(final Desktop.Action action){
		return (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(action)? Desktop.getDesktop(): null);
	}

	private static boolean hasInternetConnectivity(){
		try{
			final URL url = new URI(URL_CONNECTIVITY_TEST).toURL();
			final HttpURLConnection httpConnection = (HttpURLConnection)url.openConnection();
			final int responseCode = httpConnection.getResponseCode();
			return (responseCode == HttpURLConnection.HTTP_OK);
		}
		catch(final Exception ignored){
			return false;
		}
	}

	@SuppressWarnings("UseOfProcessBuilder")
	private static boolean runOSCommand(final ProcessBuilder builder) throws IOException, InterruptedException{
		boolean accomplished = false;
		if(builder != null){
			final Process process = builder.start();
			accomplished = (process.waitFor() == 0);
		}
		return accomplished;
	}


	/**
	 * Calculates the relative path of the target directory with respect to the base directory.
	 *
	 * @param baseDir	The base directory.
	 * @param targetDir	The target directory.
	 * @return	The relative path of the target directory with respect to the base directory.
	 */
	public static String getRelativePath(final Path baseDir, final String targetDir){
		final Path basePath = baseDir.toAbsolutePath().normalize();
		final Path targetPath = Paths.get(targetDir != null? targetDir: StringUtils.EMPTY).toAbsolutePath().normalize();
		final Path relativePath = basePath.relativize(targetPath);
		return relativePath.toString();
	}

	/**
	 * Calculates the target directory given a base directory and a relative path.
	 *
	 * @param baseDir	The base directory.
	 * @param relativeDir	The relative path.
	 * @return	The target directory.
	 */
	public static String getTargetPath(final Path baseDir, final String relativeDir){
		final Path basePath = baseDir.toAbsolutePath().normalize();
		final Path relativePath = Paths.get(relativeDir);
		final Path targetPath = basePath.resolve(relativePath).normalize();
		return targetPath.toString();
	}

}
