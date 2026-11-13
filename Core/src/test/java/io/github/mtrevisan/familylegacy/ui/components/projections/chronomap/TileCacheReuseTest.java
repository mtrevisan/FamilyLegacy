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
package io.github.mtrevisan.familylegacy.ui.components.projections.chronomap;

import org.apache.commons.io.FileUtils;
import org.jxmapviewer.viewer.Tile;
import org.jxmapviewer.viewer.TileFactoryInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;


/** A tile downloaded once is read from the disk by every later factory (that is, by every later run), not downloaded again. */
class TileCacheReuseTest{

	private static Tile load(final CancellableTileFactory factory, final int x) throws InterruptedException{
		final Tile tile = factory.getTile(x, 0, 3);
		final long deadline = System.currentTimeMillis() + 10_000;
		while(!tile.isLoaded() && System.currentTimeMillis() < deadline)
			Thread.sleep(20);
		return tile;
	}


	@Test
	void aTileAlreadyDownloadedIsNotRequestedAgain() throws IOException, InterruptedException{
		final ByteArrayOutputStream png = new ByteArrayOutputStream();
		ImageIO.write(new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB), "png", png);
		final AtomicInteger requests = new AtomicInteger();
		//a minimal HTTP server: the module of the tests does not read the one of the JDK
		final ServerSocket server = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
		final Thread acceptor = new Thread(() -> {
			while(!server.isClosed())
				try(final Socket socket = server.accept()){
					final BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.ISO_8859_1));
					String line;
					while((line = in.readLine()) != null && !line.isEmpty()){
						//the headers are of no interest
					}
					requests.incrementAndGet();
					final OutputStream out = socket.getOutputStream();
					final String crlf = "\r\n";
					out.write(("HTTP/1.1 200 OK" + crlf + "Content-Type: image/png" + crlf + "Content-Length: " + png.size() + crlf
						+ "Connection: close" + crlf + crlf).getBytes(StandardCharsets.ISO_8859_1));
					out.write(png.toByteArray());
					out.flush();
				}
				catch(final IOException ignored){
					//closed
				}
		});
		acceptor.setDaemon(true);
		acceptor.start();

		final String name = "tilecachetest-" + UUID.randomUUID();
		final File cache = new File(System.getProperty("user.home") + File.separator + ".familylegacy" + File.separator + "tiles"
			+ File.separator + name);
		try{
			final String base = "http://127.0.0.1:" + server.getLocalPort();
			final TileFactoryInfo info = new TileFactoryInfo(0, 17, 18, 256, true, true, base, "x", "y", "z"){
				@Override
				public String getName(){
					return name;
				}

				@Override
				public String getTileUrl(final int x, final int y, final int zoom){
					return base + "/" + zoom + "/" + x + "/" + y + ".png";
				}
			};

			//first run: the tile is downloaded
			CancellableTileFactory factory = HistoricalMapOverlays.createTileFactory(info);
			Assertions.assertTrue(load(factory, 1).isLoaded());
			factory.dispose();
			Assertions.assertEquals(1, requests.get());

			//later run: a new factory (a new process has nothing else) finds it on the disk
			factory = HistoricalMapOverlays.createTileFactory(info);
			Assertions.assertTrue(load(factory, 1).isLoaded());
			factory.dispose();
			Assertions.assertEquals(1, requests.get(), "the tile was downloaded again");

			//a tile dropped before it started is asked for again when it is needed, and loaded
			factory = HistoricalMapOverlays.createTileFactory(info);
			factory.getTile(2, 0, 3);
			factory.cancelPending();
			Assertions.assertTrue(load(factory, 2).isLoaded());
			factory.dispose();
		}
		finally{
			server.close();
			FileUtils.deleteQuietly(cache);
		}
	}

}
