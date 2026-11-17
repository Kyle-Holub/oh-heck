package ohheck;

import static org.junit.Assert.assertEquals;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Test;

public class RulesTextTest {

	@Test
	public void headingsAndParagraphs() {
		RulesText r = RulesText.parse("# One\nfirst line\nsecond line\n\n# Two\r\nbody two\n");
		assertEquals(2, r.headings.size());
		assertEquals("One", r.headings.get(0));
		assertEquals("first line second line", r.bodies.get(0));
		assertEquals("body two", r.bodies.get(1));
	}

	@Test
	public void theRealRulesFileHasNineSections() throws Exception {
		String text = new String(Files.readAllBytes(Paths.get("../assets/files/rules.txt")), "UTF-8");
		RulesText r = RulesText.parse(text);
		assertEquals(9, r.headings.size());
		assertEquals(9, r.bodies.size());
		assertEquals("Scoring", r.headings.get(8));
	}
}
