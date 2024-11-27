package com.matteo.AppAPIserver.Types;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class User {
	private String Name;
	private String Surname;
	private String DateOfBirth;
	private String Email;
	private BigDecimal Balance;
	
	public User(String Name, String Surname, LocalDate DateOfBirth, String Email, BigDecimal Balance) {
		this.Name = Name;
		this.Surname = Surname;
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
		this.DateOfBirth = formatter.format(DateOfBirth).toString();
		this.Email = Email;
		this.Balance = Balance;
	}
	
	public String getName() {
		return this.Name;
	}
	
	public String getSurname() {
		return this.Surname;
	}
	
	public String getDateOfBirth() {
		return this.DateOfBirth;
	}
	
	public String getEmail() {
		return this.Email;
	}
	
	public BigDecimal getBalance() {
		return this.Balance;
	}
}
