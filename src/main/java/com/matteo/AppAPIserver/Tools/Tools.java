package com.matteo.AppAPIserver.Tools;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.apache.commons.validator.routines.EmailValidator;

public class Tools {
	public static boolean isValidDate(String date, String dateFormat) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateFormat);
		try {
			LocalDate.parse(date, formatter);
			return true;
		} catch (DateTimeParseException e) {
			return false;
		}
	}
	
	public static boolean isValidEmail(String email) {
		return EmailValidator.getInstance().isValid(email);
	}
}
