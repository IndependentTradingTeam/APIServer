package com.matteo.AppAPIserver.Types;


public class Resource {
	private int ID;
	private String Symbol;
	private String Name;
	private String Exchange_MIC;
	
	public Resource(int ID, String Symbol, String Name, String Exchange_MIC) {
		this.ID = ID;
		this.Symbol = Symbol;
		this.Name = Name;
		this.Exchange_MIC = Exchange_MIC;
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
	
	public String getExchange_MIC() {
		return this.Exchange_MIC;
	}
}
