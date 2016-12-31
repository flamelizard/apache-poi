package RatingUpdate;

/**
 * Created by Tom on 12/30/2016.
 */
public class ZoneCost {

    private String costNoDPH;
    private String tarifikace;
    private String zone;

    public ZoneCost(String itemsAsCSV) {
        int id = 0;
        for (String item : itemsAsCSV.split(",")) {
            switch (id++) {
                case 2:
                    zone = item;
                    break;
                case 4:
                    tarifikace = item;
                    break;
                case 10:
                    costNoDPH = item;
                    break;
            }
        }
    }

    public String getCostNoDPH() {
        return costNoDPH;
    }

    public String getTarifikace() {
        return tarifikace;
    }

    public String getZone() {
        return zone;
    }

    @Override
    public String toString() {
        return zone + " " + tarifikace + " " + costNoDPH;
    }
}
