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
package io.github.mtrevisan.updater;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.function.BooleanSupplier;


/** Downloads the installer of a release, or another file attached to it, and checks it. */
public final class InstallerDownloader{

	private static final int BUFFER_SIZE = 64 * 1024;


	/** Told how far the download is. */
	@FunctionalInterface
	public interface Progress{

		/**
		 * @param done	The bytes received.
		 * @param total	The bytes expected, or -1 if unknown.
		 */
		void update(long done, long total);

	}


	/** The user canceled the download. */
	public static final class DownloadCancelledException extends IOException{

		public DownloadCancelledException(){
			super("Download cancelled");
		}

	}


	private final Transport transport;


	public InstallerDownloader(final Transport transport){
		this.transport = transport;
	}

	/**
	 * Downloads the installer into a file, that exists only if the download is complete and matches the published size and SHA-256.
	 *
	 * @param release	The release, that has an installer.
	 * @param directory	Where to put the installer.
	 * @param progress	Told how far the download is (called from this thread); may be {@code null}.
	 * @param cancelled	Tells whether to give up; may be {@code null}.
	 * @return	The downloaded installer.
	 * @throws DownloadCancelledException	If {@code cancelled} says so.
	 * @throws IOException	If the download fails or the file is not the published one.
	 */
	public Path download(final ReleaseInfo release, final Path directory, final Progress progress, final BooleanSupplier cancelled)
			throws IOException{
		if(!release.hasInstaller())
			throw new IOException("The release " + release.tag() + " has no installer for this system");
		//(the digest comes with the release, never from the downloaded file: without it nothing tells a good installer from a bad one)
		if(release.sha256() == null)
			throw new IOException("The release " + release.tag() + " does not publish the SHA-256 of its installer");

		return download(new ReleaseAsset(release.installerName(), release.installerUrl(), release.installerSize(), release.sha256()),
			directory, progress, cancelled);
	}

	/**
	 * Downloads a file of a release into a file, that exists only if the download is complete and matches the published size and SHA-256.
	 *
	 * @param asset	The file of the release, with its SHA-256.
	 * @param directory	Where to put the file.
	 * @param progress	Told how far the download is (called from this thread); may be {@code null}.
	 * @param cancelled	Tells whether to give up; may be {@code null}.
	 * @return	The downloaded file.
	 * @throws DownloadCancelledException	If {@code cancelled} says so.
	 * @throws IOException	If the download fails or the file is not the published one.
	 */
	public Path download(final ReleaseAsset asset, final Path directory, final Progress progress, final BooleanSupplier cancelled)
			throws IOException{
		if(!"https".equalsIgnoreCase(asset.url().getScheme()))
			throw new IOException("The file is not served over HTTPS: " + asset.url());
		if(asset.sha256() == null)
			throw new IOException("The SHA-256 of " + asset.name() + " is not published");

		final String fileName = asset.name();
		//(the name comes from the network: it must not lead outside the directory)
		if(!isPlainFileName(fileName))
			throw new IOException("Unexpected name of the file: " + fileName);

		Files.createDirectories(directory);
		final Path target = directory.resolve(fileName);
		//(a name of its own: two instances of the application must not write the same file)
		final Path partial = Files.createTempFile(directory, "installer-", ".part");
		final MessageDigest digest = newDigest();
		long done = 0;
		try(final InputStream in = transport.open(asset.url());
				final OutputStream out = Files.newOutputStream(partial)){
			final long total = asset.size();
			final byte[] buffer = new byte[BUFFER_SIZE];
			int read;
			while(true){
				if((cancelled != null && cancelled.getAsBoolean()) || Thread.currentThread().isInterrupted())
					throw new DownloadCancelledException();

				if((read = in.read(buffer)) < 0)
					break;

				out.write(buffer, 0, read);
				digest.update(buffer, 0, read);
				done += read;
				if(total >= 0 && done > total)
					throw new IOException("The file is bigger than the published " + total + " bytes");

				if(progress != null)
					progress.update(done, total);
			}
		}
		catch(final IOException | RuntimeException e){
			Files.deleteIfExists(partial);
			throw e;
		}

		try{
			if(asset.size() >= 0 && done != asset.size())
				throw new IOException("Incomplete download: " + done + " bytes of " + asset.size());
			if(!asset.sha256().equalsIgnoreCase(HexFormat.of().formatHex(digest.digest())))
				throw new IOException("The downloaded file does not match its published SHA-256");

			Files.move(partial, target, StandardCopyOption.REPLACE_EXISTING);
			return target;
		}
		catch(final IOException | RuntimeException e){
			Files.deleteIfExists(partial);
			throw e;
		}
	}

	private static boolean isPlainFileName(final String fileName){
		if(fileName == null)
			return false;
		try{
			return Path.of(fileName).getFileName().toString().equals(fileName);
		}
		catch(final InvalidPathException ignored){
			//(characters that are not allowed in a file name, such as ':' on Windows)
			return false;
		}
	}

	private static MessageDigest newDigest(){
		try{
			return MessageDigest.getInstance("SHA-256");
		}
		catch(final NoSuchAlgorithmException e){
			//(every Java platform has it)
			throw new IllegalStateException(e);
		}
	}

}
