package ohheck;

import java.util.ArrayList;
import java.util.List;

/**
 * How to play, as written in assets/files/rules.txt: a line starting with "# "
 * is a heading, and the lines after it, up to the next heading, are its text.
 */
final class RulesText {

	final List<String> headings = new ArrayList<String>();
	final List<String> bodies = new ArrayList<String>();

	static RulesText parse(String text) {
		RulesText r = new RulesText();
		StringBuilder body = null;
		for (String line : text.split("\\r?\\n")) {
			line = line.trim();
			if (line.startsWith("# ")) {
				if (body != null) {
					r.bodies.add(body.toString());
				}
				r.headings.add(line.substring(2).trim());
				body = new StringBuilder();
			} else if (body != null && !line.isEmpty()) {
				body.append(body.length() > 0 ? " " : "").append(line);
			}
		}
		if (body != null) {
			r.bodies.add(body.toString());
		}
		return r;
	}
}
