
package models;

import java.time.LocalDateTime;
// Add this inner static class to your controller (or create a separate model file)
public  class payementdetails {
    private long vid;
    private long fvid;
    private double amountPaid;
    private String note;
    private boolean active;
    private LocalDateTime createdOn;

    public payementdetails(long vid, long fvid, double amountPaid, String note, boolean active, LocalDateTime createdOn) {
        this.vid = vid;
        this.fvid = fvid;
        this.amountPaid = amountPaid;
        this.note = note;
        this.active = active;
        this.createdOn = createdOn;
    }

    public long getVid() { return vid; }
    public long getFvid() { return fvid; }
    public double getAmountPaid() { return amountPaid; }
    public String getNote() { return note; }
    public boolean isActive() { return active; }
    public LocalDateTime getCreatedOn() { return createdOn; }
}
