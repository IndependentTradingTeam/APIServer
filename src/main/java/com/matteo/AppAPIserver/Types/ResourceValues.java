package com.matteo.AppAPIserver.Types;

import java.math.BigDecimal;

public class ResourceValues {
    private BigDecimal CurrentPrice;
	private BigDecimal Change;
	private BigDecimal PercentChange;
	private BigDecimal Hight;
	private BigDecimal Low;
	private BigDecimal Open;
	private BigDecimal PreviousClose;

    public ResourceValues() {
        this(null, null, null, null, null, null, null);
    }

    public ResourceValues(BigDecimal CurrentPrice, BigDecimal Change, BigDecimal PercentChange, BigDecimal Hight, BigDecimal Low, BigDecimal Open, BigDecimal PreviousClose) {
        this.CurrentPrice = CurrentPrice;
        this.Change = Change;
        this.PercentChange = PercentChange;
        this.Hight = Hight;
        this.Low = Low;
        this.Open = Open;
        this.PreviousClose = PreviousClose;
    }

    public BigDecimal getCurrentPrice() {
		return this.CurrentPrice;
	}

	public BigDecimal getChange() {
		return this.Change;
	}

	public BigDecimal getPercentChange() {
		return this.PercentChange;
	}

	public BigDecimal getHight() {
		return this.Hight;
	}

	public BigDecimal getLow() {
		return this.Low;
	}

	public BigDecimal getOpen() {
		return this.Open;
	}

	public BigDecimal getPreviousClose() {
		return this.PreviousClose;
	}
}
