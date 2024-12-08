package com.matteo.AppAPIserver.Types;


public class Resource {
	private int ID;
	private String Symbol;
	private String Name;
	
	public Resource(int ID, String Symbol, String Name) {
		this.ID = ID;
		this.Symbol = Symbol;
		this.Name = Name;
	}
	
	public int getID() {
		return this.ID;
	}
	
	public String getSymbol() {
		return this.Symbol;
	}
	
	public String getName() {
		return this.Name;
	}
	
}
