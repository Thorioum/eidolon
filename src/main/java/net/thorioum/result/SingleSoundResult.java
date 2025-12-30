package net.thorioum.result;

public record SingleSoundResult(String name, double pitch, double volume, double similarity) {

    public String asCommand() {
        String p = String.format("%.8f", pitch);
        String v = String.format("%.8f", volume);
        return "/playsound " + name + " record @a[tag=!nomusic,tag=!nm] ~ -9999 ~ 1 " + p + " " + v;
    }

}
