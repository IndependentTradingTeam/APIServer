package com.matteo.AppAPIserver.Types;

public class Exchange {
	private String MIC;
	private String Name;
	
	public Exchange(String MIC, String Name) {
		this.MIC = MIC;
		this.Name = Name;
	}
	
	public String getMIC() {
		return this.MIC;
	}
	
	public String getName() {
		return this.Name;
	}
}
