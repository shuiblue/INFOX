package Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;

public class JensRunner {
	
	static PrintStream out = System.out; 

	static boolean coreFailure = true;

	public static void main(String[] args) {
		System.out.println("Processors: " + Runtime.getRuntime().availableProcessors());
		System.out.println("Execute: ");
		List<String> commands = new LinkedList<>();
		commands.add("ulimit");
		commands.add("-c");
		commands.add("unlimited");


		commands.add("./gradlew");
		commands.add("run");

		while (coreFailure) {
			coreFailure = false;
			System.out.println(commands);
			process(commands);
		}

	}

	private static void process(List<String> commands) {
		ProcessBuilder processBuilder = new ProcessBuilder(commands);
		try {
			Process process = processBuilder.start();
			try (BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));
				 BufferedReader error = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
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
							if (exitValue == 134||exitValue==1) {
								System.out.println("Exit value = " + exitValue);
								coreFailure = true;
								return;
							}

							throw new IOException("The process doesn't finish normally (exit=" + exitValue + ")!");
						}
						break;
					} catch (IllegalThreadStateException e) {
						e.printStackTrace();
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
