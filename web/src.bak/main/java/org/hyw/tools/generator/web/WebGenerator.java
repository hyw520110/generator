package org.hyw.tools.generator.web;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.io.IOUtils;

public class WebGenerator {
	public static void main(String[] args) throws IOException {
		String dir = new File("").getAbsolutePath();
		Process p=Runtime.getRuntime().exec("cd "+dir+" && npm install ");
		try (InputStream in = p.getInputStream();) {
			System.out.println(IOUtils.readLines(new InputStreamReader(in, "GBK")));
		}
	}
}
