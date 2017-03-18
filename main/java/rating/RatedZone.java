package rating;

/**
 * Created by Tom on 1/22/2017.
 */
public class RatedZone {
    private final String zone;
    private final String tarification;
    private final String price;
    private String service; // set only for SMS, MMS

    public RatedZone(String zone, String tarification, String price, String service) {
        this.zone = zone;
        this.tarification = tarification;
        this.price = price;
        this.service = service;
    }

    public RatedZone(String zone, String tarification, String price) {
        this(zone, tarification, price, "N/A");
    }

    public String getTarification() {
        return tarification;
    }

    public String getPrice() {
        return price;
    }

    public String getZone() {
        return zone;
    }


    public String getService() {
        return service;
    }

    public String toString() {
        return String.format("%s-[%s] %s %s",
                getService(), getZone(), getTarification(), getPrice());
    }
}
