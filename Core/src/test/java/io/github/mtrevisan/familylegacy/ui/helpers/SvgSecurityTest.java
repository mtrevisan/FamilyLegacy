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
package io.github.mtrevisan.familylegacy.ui.helpers;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * An SVG chosen by the user is rendered by Batik (through {@code imageio-batik}): it must not run scripts and must not reach
 * anything outside the file (a web address, a local file).
 */
class SvgSecurityTest{

	private static final String NS = "xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\"";


	@Test
	void anExternalImageIsNotFetched(@TempDir final Path directory) throws Exception{
		final AtomicInteger requests = new AtomicInteger();
		try(final ServerSocket server = new ServerSocket(0, 5, InetAddress.getLoopbackAddress())){
			final Thread acceptor = new Thread(() -> {
				try{
					while(true){
						server.accept().close();
						requests.incrementAndGet();
					}
				}
				catch(final IOException ignored){
					//closed
				}
			});
			acceptor.setDaemon(true);
			acceptor.start();

			final String url = "http://127.0.0.1:" + server.getLocalPort() + "/x.png";
			final Path svg = directory.resolve("external.svg");
			Files.writeString(svg, "<svg " + NS + " width=\"20\" height=\"20\"><image width=\"20\" height=\"20\" xlink:href=\"" + url + "\"/>"
				+ "<rect width=\"20\" height=\"20\" style=\"fill:url(" + url + "#a)\"/></svg>", StandardCharsets.UTF_8);

			read(svg);
			Thread.sleep(300L);

			Assertions.assertEquals(0, requests.get(), "the SVG reached an external address");
		}
	}

	@Test
	void anExternalEntityIsNotResolved(@TempDir final Path directory) throws IOException{
		final Path secret = directory.resolve("secret.txt");
		Files.writeString(secret, "SECRET", StandardCharsets.UTF_8);
		final Path svg = directory.resolve("entity.svg");
		Files.writeString(svg, "<?xml version=\"1.0\"?><!DOCTYPE svg [<!ENTITY x SYSTEM \"" + secret.toUri() + "\">]>"
			+ "<svg " + NS + " width=\"200\" height=\"20\"><text y=\"15\">&x;</text></svg>", StandardCharsets.UTF_8);

		final BufferedImage withEntity = read(svg);
		final Path plain = directory.resolve("plain.svg");
		Files.writeString(plain, "<svg " + NS + " width=\"200\" height=\"20\"><text y=\"15\"></text></svg>", StandardCharsets.UTF_8);
		final BufferedImage without = read(plain);

		//no text drawn in either case: the entity brought nothing in
		Assertions.assertTrue(withEntity == null || without == null || sameBlank(withEntity, without), "the entity was resolved");
	}

	@Test
	void aScriptIsNotExecuted(@TempDir final Path directory) throws IOException{
		final Path svg = directory.resolve("script.svg");
		Files.writeString(svg, "<svg " + NS + " width=\"20\" height=\"20\" onload=\"document.documentElement.setAttribute('width','99')\">"
			+ "<script type=\"text/ecmascript\">document.documentElement.setAttribute('width','99')</script>"
			+ "<rect width=\"20\" height=\"20\"/></svg>", StandardCharsets.UTF_8);

		final BufferedImage image = read(svg);

		Assertions.assertTrue(image == null || image.getWidth() == 20, "the script ran");
	}


	@Test
	void anHugeDeclaredSizeIsRefused(@TempDir final Path directory) throws IOException{
		final Path svg = directory.resolve("huge.svg");
		Files.writeString(svg, "<svg " + NS + " width=\"100000\" height=\"100000\"><rect width=\"20\" height=\"20\"/></svg>",
			StandardCharsets.UTF_8);

		Assertions.assertThrows(IOException.class, () -> ResourceHelper.readBufferedImage(svg.toFile()));
	}

	@Test
	void anHugeFileIsRefused(@TempDir final Path directory) throws IOException{
		final Path svg = directory.resolve("big.svg");
		Files.writeString(svg, "<svg " + NS + " width=\"20\" height=\"20\"><!--" + "x".repeat((int)ResourceHelper.SVG_MAX_BYTES)
			+ "--></svg>", StandardCharsets.UTF_8);

		Assertions.assertThrows(IOException.class, () -> ResourceHelper.readBufferedImage(svg.toFile()));
	}

	@Test
	void aNormalSizeIsAccepted(@TempDir final Path directory) throws IOException{
		final Path svg = directory.resolve("ok.svg");
		Files.writeString(svg, "<svg " + NS + " width=\"20\" height=\"20\"><rect width=\"20\" height=\"20\"/></svg>",
			StandardCharsets.UTF_8);

		final BufferedImage image = ResourceHelper.readBufferedImage(svg.toFile());

		Assertions.assertEquals(20, image.getWidth());
	}


	private static BufferedImage read(final Path svg) throws IOException{
		try{
			return ResourceHelper.readBufferedImage(svg.toFile());
		}
		catch(final IOException | RuntimeException ignored){
			//refusing the file is a safe outcome too
			return null;
		}
	}

	private static boolean sameBlank(final BufferedImage a, final BufferedImage b){
		if(a.getWidth() != b.getWidth() || a.getHeight() != b.getHeight())
			return false;
		for(int y = 0; y < a.getHeight(); y ++)
			for(int x = 0; x < a.getWidth(); x ++)
				if(a.getRGB(x, y) != b.getRGB(x, y))
					return false;
		return true;
	}

}
