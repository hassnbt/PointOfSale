package models;

public class Area {
    private long areaid;
    private String name;

    public Area(long areaid, String name) {
        this.areaid = areaid;
        this.name = name;
    }

    public long getAreaid() {
        return areaid;
    }

    public String getName() {
        return name;
    }

    // This is used by the ComboBox to display the name.
    @Override
    public String toString() {
        return name;
    }
}
