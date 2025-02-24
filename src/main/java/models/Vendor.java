package models;

import java.time.LocalDateTime;

public class Vendor {
    private long vid;
    private String name;
    private String phoneNo;
    private String address;
    private LocalDateTime createdOn;
    private boolean active;

    public Vendor(long vid, String name, String phoneNo, String address, LocalDateTime createdOn, boolean active) {
        this.vid = vid;
        this.name = name;
        this.phoneNo = phoneNo;
        this.address = address;
        this.createdOn = createdOn;
        this.active = active;
    }
    public Vendor(long vid, String name) {
        this.vid = vid;
        this.name = name;

    }

    public long getVid() {
        return vid;
    }

    public void setVid(long vid) {
        this.vid = vid;
    }

    public String getName() {
        return name;
    }
    @Override
    public String toString() {
        return name; // Display only the name in the ComboBox
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNo() {
        return phoneNo;
    }

    public void setPhoneNo(String phoneNo) {
        this.phoneNo = phoneNo;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public LocalDateTime getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(LocalDateTime createdOn) {
        this.createdOn = createdOn;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
