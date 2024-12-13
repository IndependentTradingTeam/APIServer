package com.matteo.AppAPIserver.Types;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public class Action {
    private int ID;
    private int Resource_ID;
    private String Resource_Symbol;
    private BigDecimal Quantity;
    private LocalDate BuyDate;
    private LocalTime BuyTime;
    private LocalDate EndDate;
    private LocalTime EndTime;
    private boolean Finished;
    
    public Action(int ID, int Resource_ID, String Resource_Symbol, BigDecimal Quantity, LocalDate BuyDate, LocalTime BuyTime, LocalDate EndDate, LocalTime EndTime) {
        this.ID = ID;
        this.Resource_ID = Resource_ID;
        this.Resource_Symbol = Resource_Symbol;
        this.Quantity = Quantity;
        this.BuyDate = BuyDate;
        this.BuyTime = BuyTime;
        this.EndDate = EndDate;
        this.EndTime = EndTime;
        if(EndDate != null && EndTime != null) {
            this.Finished = true;
        } else {
            this.Finished = false;
        }
    }

    public int getID() {
        return this.ID;
    }

    public int getResource_ID() {
        return this.Resource_ID;
    }

    public String getResource_Symbol() {
        return this.Resource_Symbol;
    }

    public BigDecimal getQuantity() {
        return this.Quantity;
    }

    public LocalDate getBuyDate() {
        return this.BuyDate;
    }

    public LocalTime getBuyTime() {
        return this.BuyTime;
    }

    public LocalDate getEndDate() {
        return this.EndDate;
    }

    public LocalTime getEndTime() {
        return this.EndTime;
    }

    public boolean isFinished() {
        return this.Finished;
    }
}
