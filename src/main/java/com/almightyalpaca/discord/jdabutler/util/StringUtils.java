package com.almightyalpaca.discord.jdabutler.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class StringUtils {

	public static void replaceAll(final StringBuilder builder, final String from, final String to) {
		int index;
		while ((index = builder.indexOf(from)) != -1) {
			builder.replace(index, index + from.length(), to);
		}

	}

	public static String replaceFirst(final String text, final String searchString, final String replacement) {
		return org.apache.commons.lang3.StringUtils.replaceOnce(text, searchString, replacement);
	}

	/**
	 * Thanks StackOverflow
	 */
	public static String replaceLast(final String text, final String regex, final String replacement) {
		return text.replaceFirst("(?s)(.*)" + regex, "$1" + replacement);
	}

	public static String[] split(String string, final int lenth, final String split) {
		Objects.requireNonNull(string);
		if (string.length() == 0) {
			return new String[] {};
		} else if (string.length() == 1) {
			return new String[] { string };
		} else if (string.length() <= lenth) {
			return new String[] { string };
		}
		final List<String> strings = new ArrayList<>();

		while (string.length() > lenth) {
			final String current = string.substring(0, lenth + split.length());

			final int index = current.lastIndexOf(split);

			if (index == -1) {
				throw new UnsupportedOperationException("One or more substrings were too long!");
			}

			final String substring = current.substring(0, index);

			strings.add(substring);
			string = StringUtils.replaceFirst(string, substring + split, "");

		}

		return strings.toArray(new String[strings.size()]);
	}

	public static String toPrettyString(final Iterable<?> collection) {
		String string = "";

		for (final Object object : collection) {
			string += Objects.toString(object) + ", ";
		}
		return StringUtils.replaceLast(string, ", ", "");
	}

	public static String exceptionToString(Exception e){
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        pw.flush();
        String s = sw.toString();
        pw.close();
        return s;
    }
}
