package models;

import java.time.LocalDateTime;

public class EmployeeSalary {
    private long sid;
    private long empId;
    private double amount;
    private LocalDateTime createdOn;
    private boolean active;

    public EmployeeSalary(long sid, long empId, double amount, LocalDateTime createdOn, boolean active) {
        this.sid = sid;
        this.empId = empId;
        this.amount = amount;
        this.createdOn = createdOn;
        this.active = active;
    }


    public long getEmpId() {
        return empId;
    }

    public double getAmount() {
        return amount;
    }

    public LocalDateTime getCreatedOn() {
        return createdOn;
    }

    public boolean isActive() {
        return active;
    }

    public long getSid() {
        return sid;
    }

    public void setSid(long sid) {
        this.sid = sid;
    }
}
