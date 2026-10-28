package io.github.mtrevisan.familylegacy.ui.helpers;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.swing.JPanel;
import java.awt.event.KeyEvent;


class DialogEscapeSupportTest{

	private static final JPanel SOURCE = new JPanel();

	private static KeyEvent key(final int id, final long when, final int keyCode){
		return new KeyEvent(SOURCE, id, when, 0, keyCode, KeyEvent.CHAR_UNDEFINED);
	}

	private static KeyEvent press(final long when){
		return key(KeyEvent.KEY_PRESSED, when, KeyEvent.VK_ESCAPE);
	}

	private static KeyEvent release(final long when){
		return key(KeyEvent.KEY_RELEASED, when, KeyEvent.VK_ESCAPE);
	}


	@Test
	void theFirstPressReachesTheDialog(){
		final DialogEscapeSupport.Gate gate = new DialogEscapeSupport.Gate();

		Assertions.assertFalse(gate.consumes(press(1_000)));
	}

	@Test
	void theRepeatedPressesOfAHeldKeyAreConsumed(){
		final DialogEscapeSupport.Gate gate = new DialogEscapeSupport.Gate();

		Assertions.assertFalse(gate.consumes(press(1_000)));
		//the key is held: after the delay it repeats, and the repetitions would close the dialogs below the one that is closed
		Assertions.assertTrue(gate.consumes(press(1_500)));
		Assertions.assertTrue(gate.consumes(press(1_533)));
		Assertions.assertTrue(gate.consumes(press(1_566)));
	}

	@Test
	void aHeldKeyIsOnePressHoweverLongItIsHeld(){
		final DialogEscapeSupport.Gate gate = new DialogEscapeSupport.Gate();
		gate.consumes(press(0));

		//held for ten seconds, repeating every 33 ms
		for(long when = 33; when <= 10_000; when += 33)
			Assertions.assertTrue(gate.consumes(press(when)), "at " + when);
	}

	@Test
	void aNewPressAfterTheReleaseReachesTheNextDialog(){
		final DialogEscapeSupport.Gate gate = new DialogEscapeSupport.Gate();

		Assertions.assertFalse(gate.consumes(press(1_000)));
		Assertions.assertFalse(gate.consumes(release(1_100)));
		Assertions.assertFalse(gate.consumes(press(1_300)));
		//and that one can be held too
		Assertions.assertTrue(gate.consumes(press(1_800)));
		Assertions.assertFalse(gate.consumes(release(1_900)));
		Assertions.assertFalse(gate.consumes(press(2_000)));
	}

	@Test
	void aLostReleaseDoesNotBlockTheKeyForever(){
		final DialogEscapeSupport.Gate gate = new DialogEscapeSupport.Gate();
		gate.consumes(press(1_000));

		//the window lost the focus with the key down, the release never came: a press much later is a new one
		Assertions.assertFalse(gate.consumes(press(1_000 + DialogEscapeSupport.REPEAT_WINDOW_MILLIS + 1)));
	}

	@Test
	void theOtherKeysAreNeverConsumed(){
		final DialogEscapeSupport.Gate gate = new DialogEscapeSupport.Gate();

		for(int i = 0; i < 5; i ++){
			Assertions.assertFalse(gate.consumes(key(KeyEvent.KEY_PRESSED, 1_000 + i, KeyEvent.VK_ENTER)));
			Assertions.assertFalse(gate.consumes(key(KeyEvent.KEY_PRESSED, 1_000 + i, KeyEvent.VK_A)));
		}
		//a key typed is not a press
		Assertions.assertFalse(gate.consumes(new KeyEvent(SOURCE, KeyEvent.KEY_TYPED, 1_000, 0, KeyEvent.VK_UNDEFINED, (char)27)));
		//and the keys do not disturb the state of ESC
		Assertions.assertFalse(gate.consumes(press(2_000)));
		Assertions.assertFalse(gate.consumes(key(KeyEvent.KEY_PRESSED, 2_100, KeyEvent.VK_ENTER)));
		Assertions.assertTrue(gate.consumes(press(2_200)));
	}

}
