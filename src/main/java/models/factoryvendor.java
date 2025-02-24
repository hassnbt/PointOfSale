package models;

import java.time.LocalDateTime;

public class factoryvendor {
    private long id;
    private String name;
    private String phone;
    private LocalDateTime createdOn;
    private boolean active;

    public factoryvendor(long id, String name, String phone, LocalDateTime createdOn, boolean active) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.createdOn = createdOn;
        this.active = active;
    }

    // Getters and setters
    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public LocalDateTime getCreatedOn() {
        return createdOn;
    }

    public boolean isActive() {
        return active;
    }
}
