package models;

import java.time.LocalDateTime;

public class Employee {
    private long eid;
    private String name;
    private LocalDateTime joiningDate;
    private boolean active;
    private String phoneNumber;

    public Employee(long eid, String name, LocalDateTime joiningDate, boolean active, String phoneNumber) {
        this.eid = eid;
        this.name = name;
        this.joiningDate = joiningDate;
        this.active = active;
        this.phoneNumber = phoneNumber;
    }

    public Employee(long eid, String name) {
        this.eid = eid;
        this.name = name;

    }

    public long getEid() {
        return eid;
    }

    public String getName() {
        return name;
    }

    public LocalDateTime getJoiningDate() {
        return joiningDate;
    }

    public boolean isActive() {
        return active;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }
    @Override
    public String toString() {
        return name; // Display only the name in the ComboBox
    }
    // Setters (if needed)
    public void setEid(long eid) { this.eid = eid; }
    public void setName(String name) { this.name = name; }
    public void setJoiningDate(LocalDateTime joiningDate) { this.joiningDate = joiningDate; }
    public void setActive(boolean active) { this.active = active; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
}
