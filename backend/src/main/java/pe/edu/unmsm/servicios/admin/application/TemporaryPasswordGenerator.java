package pe.edu.unmsm.servicios.admin.application;

import java.security.SecureRandom;

/** Random 12-character password with at least one letter and one digit, as registration requires. */
public class TemporaryPasswordGenerator {

	private static final String LETTERS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz";
	private static final String DIGITS = "23456789";
	private static final String ALPHABET = LETTERS + DIGITS;
	private static final int LENGTH = 12;

	private final SecureRandom random = new SecureRandom();

	public String generate() {
		char[] password = new char[LENGTH];
		password[0] = pick(LETTERS);
		password[1] = pick(DIGITS);
		for (int i = 2; i < LENGTH; i++) {
			password[i] = pick(ALPHABET);
		}
		for (int i = LENGTH - 1; i > 0; i--) {
			int j = random.nextInt(i + 1);
			char swap = password[i];
			password[i] = password[j];
			password[j] = swap;
		}
		return new String(password);
	}

	private char pick(String source) {
		return source.charAt(random.nextInt(source.length()));
	}

}
