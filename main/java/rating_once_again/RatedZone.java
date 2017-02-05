package rating_once_again;

/**
 * Created by Tom on 1/22/2017.
 */
public class RatedZone {
    private final String zone;
    private final String tarification;
    private final String voicePrice;
//    private String smsPrice;

    public RatedZone(String zone, String tarification, String voicePrice) {

        this.zone = zone;
        this.tarification = tarification;
        this.voicePrice = voicePrice;
    }

    public String getTarification() {
        return tarification;
    }

    public String getVoicePrice() {
        return voicePrice;
    }

    public String getZone() {
        return zone;
    }

//    filter for 1+! tarification to get object for SMS price
//    public String getSmsPrice() {
//        return smsPrice;
//    }
//
//    public void setSmsPrice(String smsPrice) {
//        this.smsPrice = smsPrice;
//    }

    public String toString() {
        return String.format("[%s] %s %s",
                getZone(), getTarification(), getVoicePrice());
    }
}
