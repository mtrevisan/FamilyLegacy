package io.github.mtrevisan.familylegacy.ui.dialogs;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;


/**
 * A guard for the dialogs to come: every dialog class must give ESC a meaning, so that no dialog has to be closed with the mouse.
 * <p>
 * It reads the sources, because the dialogs that need arguments cannot all be built here (the ones that can are closed with ESC for
 * real in {@code DialogsEscapeTest}). A dialog that is not cancelable on purpose has to be listed below, with the reason.
 * </p>
 */
class EveryDialogClosesWithEscTest{

	private static final Path SOURCES = Path.of("src", "main", "java");

	/** A class that is a dialog. */
	private static final Pattern DIALOG = Pattern.compile("extends\\s+(JDialog|BaseRecordDialog)\\b|new\\s+JDialog\\(");
	/** What gives ESC a meaning: the helper of the tools, the button panels (their cancel), a binding of the key. */
	private static final Pattern ESCAPE = Pattern.compile("ToolDialogs\\.installEscapeToClose|createButtonPanel|"
		+ "createNewSelectCancelButtonPanel|ESCAPE_STROKE|VK_ESCAPE|extends\\s+BaseRecordDialog\\b");

	/** The dialogs that must not be closed with ESC: they show a long operation that cannot be canceled. */
	private static final Set<String> NOT_CANCELABLE = Set.of("ProgressDialog.java");


	@Test
	void everyDialogGivesEscAMeaning() throws IOException{
		Assumptions.assumeTrue(Files.isDirectory(SOURCES), "The sources are not here");
		final List<String> without = new ArrayList<>();
		int dialogs = 0;

		try(final Stream<Path> files = Files.walk(SOURCES)){
			for(final Path file : files.filter(f -> f.toString().endsWith(".java")).toList()){
				final String source = Files.readString(file, StandardCharsets.UTF_8);
				if(!DIALOG.matcher(source).find() || file.toString().contains("handlers"))
					continue;

				dialogs ++;
				if(!ESCAPE.matcher(source).find() && !NOT_CANCELABLE.contains(file.getFileName().toString()))
					without.add(SOURCES.relativize(file).toString());
			}
		}

		Assertions.assertTrue(dialogs > 40, "only " + dialogs + " dialogs found: is the scan right?");
		Assertions.assertTrue(without.isEmpty(), "dialogs that ESC does not close: " + without);
	}

}
