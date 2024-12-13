package com.matteo.AppAPIserver.Types;

import java.math.BigDecimal;
import java.time.LocalDate;

public class User {
	private String Name;
	private String Surname;
	private LocalDate DateOfBirth;
	private String Email;
	private BigDecimal Balance;
	
	public User(String Name, String Surname, LocalDate DateOfBirth, String Email, BigDecimal Balance) {
		this.Name = Name;
		this.Surname = Surname;
		this.DateOfBirth = DateOfBirth;
		this.Email = Email;
		this.Balance = Balance;
	}
	
	public String getName() {
		return this.Name;
	}
	
	public String getSurname() {
		return this.Surname;
	}
	
	public LocalDate getDateOfBirth() {
		return this.DateOfBirth;
	}
	
	public String getEmail() {
		return this.Email;
	}
	
	public BigDecimal getBalance() {
		return this.Balance;
	}
}
