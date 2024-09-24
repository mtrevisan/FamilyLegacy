/**
 * Copyright (c) 2024 Mauro Trevisan
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;


public class LZMAManagerTest{

	@Test
	void compressAndDecompress() throws IOException{
		final String text = """
			4:77a497f9-86a1-49de-95ab-25ec5dfe090e:0|citation|{"extract_type":"transcript","extract_locale":"en-US","extract":"text 1","location":"here","id":1,"source_id":1}
			4:77a497f9-86a1-49de-95ab-25ec5dfe090e:1|assertion|{"role":"father","reference_id":1,"reference_table":"table","citation_id":1,"certainty":"certain","credibility":"direct and primary evidence used, or by dominance of the evidence","id":1}
			4:77a497f9-86a1-49de-95ab-25ec5dfe090e:2|source|{"identifier":"source 1","repository_id":1,"id":1}
			4:77a497f9-86a1-49de-95ab-25ec5dfe090e:3|repository|{"identifier":"repo 1","id":1,"type":"public library"}
			4:77a497f9-86a1-49de-95ab-25ec5dfe090e:4|localized_text|{"transcription":"IPA","id":1,"text":"text 1","transcription_type":"romanized","type":"original","locale":"it"}
			4:77a497f9-86a1-49de-95ab-25ec5dfe090e:5|localized_text|{"transcription":"kana","id":2,"text":"text 2","transcription_type":"romanized","type":"original","locale":"en"}
			4:77a497f9-86a1-49de-95ab-25ec5dfe090e:6|note|{"note":"note 1","reference_id":1,"reference_table":"person","id":1}
			4:77a497f9-86a1-49de-95ab-25ec5dfe090e:7|note|{"note":"note 2","reference_id":1,"reference_table":"citation","id":2}
			4:77a497f9-86a1-49de-95ab-25ec5dfe090e:8|restriction|{"reference_id":1,"reference_table":"citation","restriction":"confidential","id":1}
			4:77a497f9-86a1-49de-95ab-25ec5dfe090e:4|4:77a497f9-86a1-49de-95ab-25ec5dfe090e:0|for|{"onDeleteEnd":"CASCADE","type":"extract","onDeleteStart":"RELATIONSHIP_ONLY"}
			""";

		final byte[] compressed = LZMAManager.compress(text);
		final String compressedHex = LZMAManager.toHexString(compressed);
		final String decompressed = LZMAManager.decompress(compressed);

		Assertions.assertEquals("FD377A585A000004E6D6B4460200210114000000FFE7EC09E005C601E85D001A0E834F1206058C959A0325BDAF73301AEBDC92FE7CCEC25D7150D72D2D8CA7599889FFA5B3FB1874F6922751B9B56922914D9E9DBE9B68F2354FB83C44AA4B2F9B60759DDB9E10425658BBD0A0D08A4BD77F00DFB31FF5DC4C0F6221E9F45F7E025D937809131F114C10F07032C33750F6FA909C9DB9DA43CE2EF403F71401430E6295A57B7ABB71076492E65A1A3CB5AE393EA46194DFF5435A4AFCA355D740EEF2EE62285D1979629C1F0EEADE59C6BD116A48B9341327698CD49392D9068822D190ADCAF1C64423F8A64AAA664A15102B785B07F98A0EA95B019E4012BA1BDCD6A9451EC2494D31BB6DDF51F4E93DDF514225E8F510BFF590DC33115291344399F68B73BC522D5C03FAA0BCF56CBF53157C739D4FAF2548F83322465CC5118AF23C4405D68C02EBDDE4E47876FF3CAA5DC2B79274AF4E030374AB2A71C4D001ADDB97663D27B99EB37986AFF5A54A3E7CEBA9228AF62E45D11FFD21F0B5158AC55CADEEEFF8A5D4CF7C995CCC0D7DD03369571EA91D9C26FAB41C5B7B189233EF311AE39A200D26CCB3C279BF54C31EC3BF38A9BBBC5B7371814D1932F3075A6F653BCB3719F9D1B54FF64FADEB32F68DCF9E56064F136CC64C0EF91A5945EAFE671399ED88D0272EEBC94D4A626E95F09721B155005DA28B51ED9E7F88301B5807F61D7A0000D9EC6E3689EEC26400018404C70B00008D1D3B54B1C467FB020000000004595A", compressedHex);
		Assertions.assertEquals(text, decompressed);
	}

}
