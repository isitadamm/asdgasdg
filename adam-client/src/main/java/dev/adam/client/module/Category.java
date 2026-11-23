package dev.adam.client.module;

public enum Category {
    BLOCKS("Blocks", "Highlight chosen blocks through walls."),
    CAM("Cam", "Detach your camera and look around."),
    WINGS("Wings", "Fly in the air, or on a boat."),
    SWORD("Sword", "Reach, kill aura, crits and more."),
    PROTECTION("Protection", "See who is nearby and who is spectating.");

    public final String label;
    public final String blurb;

    Category(String label, String blurb) {
        this.label = label;
        this.blurb = blurb;
    }
}
