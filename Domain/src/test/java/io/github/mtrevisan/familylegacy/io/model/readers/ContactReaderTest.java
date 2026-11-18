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
package io.github.mtrevisan.familylegacy.io.model.readers;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class ContactReaderTest{

	@Test
	void emailAddress(){
		Assertions.assertEquals(ContactReader.ENUM_TYPE_EMAIL, ContactReader.inferType("mauro.simioni@example.com"));
		Assertions.assertEquals(ContactReader.ENUM_TYPE_EMAIL, ContactReader.inferType("mailto:someone@example.org"));
		Assertions.assertNull(ContactReader.inferType("not an@email"));
	}

	@Test
	void phoneNumber(){
		Assertions.assertEquals(ContactReader.ENUM_TYPE_PHONE, ContactReader.inferType("+39 049 123 4567"));
		Assertions.assertEquals(ContactReader.ENUM_TYPE_PHONE, ContactReader.inferType("(049) 123-4567"));
		Assertions.assertEquals(ContactReader.ENUM_TYPE_PHONE, ContactReader.inferType("tel:+390491234567"));
		//a date or a year is not a telephone number
		Assertions.assertNull(ContactReader.inferType("12.09.1954"));
		Assertions.assertNull(ContactReader.inferType("1954"));
	}

	@Test
	void webAddress(){
		Assertions.assertEquals(ContactReader.ENUM_TYPE_WEBSITE, ContactReader.inferType("https://www.example.com/family"));
		Assertions.assertEquals(ContactReader.ENUM_TYPE_WEBSITE, ContactReader.inferType("www.example.com"));
		Assertions.assertEquals(ContactReader.ENUM_TYPE_SOCIAL, ContactReader.inferType("https://www.facebook.com/someone"));
		Assertions.assertEquals(ContactReader.ENUM_TYPE_MESSAGING, ContactReader.inferType("https://t.me/someone"));
		Assertions.assertEquals(ContactReader.ENUM_TYPE_BLOG, ContactReader.inferType("https://roots.blogspot.com/2020"));
	}

	@Test
	void postalAddress(){
		Assertions.assertEquals(ContactReader.ENUM_TYPE_POSTAL, ContactReader.inferType("Via Roma 12, 35100 Padova"));
		Assertions.assertEquals(ContactReader.ENUM_TYPE_POSTAL, ContactReader.inferType("10 Downing Street, London"));
		Assertions.assertNull(ContactReader.inferType("Padova"));
		Assertions.assertNull(ContactReader.inferType(null));
	}

}
