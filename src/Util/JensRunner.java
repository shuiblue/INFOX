package Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;

public class JensRunner {
	
	static PrintStream out = System.out; 

	public static void main(String[] args) {
		System.out.println("Execute: ");
		List<String> commands = new LinkedList<>();
		commands.add("./gradlew");
		commands.add("run");
		System.out.println(commands);

		process(commands);
		out.println();
		out.flush();
		out.close();
	}

	private static void process(List<String> commands) {
		ProcessBuilder processBuilder = new ProcessBuilder(commands);
		BufferedReader input = null;
		BufferedReader error = null;
		try {
			Process process = processBuilder.start();
			input = new BufferedReader(new InputStreamReader(process.getInputStream()));
			error = new BufferedReader(new InputStreamReader(process.getErrorStream()));

			while (true) {
				try {
					String line;
					while ((line = input.readLine()) != null) {
						System.out.println(line);
					}
					while ((line = error.readLine()) != null) {
						System.out.println(line);
					}

					try {
						process.waitFor();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					int exitValue = process.exitValue();
					if (exitValue != 0) {
						throw new IOException("The process doesn't finish normally (exit=" + exitValue + ")!");
					}
					break;
				} catch (IllegalThreadStateException e) {
					e.printStackTrace();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (input != null) {
					input.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (error != null) {
					try {
						error.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
}
