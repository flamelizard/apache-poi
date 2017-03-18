package rating;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

public class PriceCalculator {
    private final Float priceVAT;
    private final String tarification;
    private String serviceType;
    private Logger log = Logger.getLogger(PriceCalculator.class.getName());

    public PriceCalculator(Float priceVAT, String tarification, String serviceType) {
        this.priceVAT = priceVAT;
        this.tarification = tarification;
        this.serviceType = serviceType;
        log.setLevel(Level.INFO);
    }

    public static String formatNumberEU(Float number) {
        DecimalFormat fmt = (DecimalFormat) NumberFormat.getInstance(Locale.FRENCH);
        return fmt.format(number);
    }

    public Float getPriceVAT() {
        return priceVAT;
    }

    private Float roundAsRequired(Float number) {
        // force decimal separator to "." for internal operations
        DecimalFormat numFmt = (DecimalFormat) NumberFormat.getInstance(Locale.US);
        numFmt.applyPattern(".###");
        numFmt.setRoundingMode(RoundingMode.HALF_UP);
        return Float.valueOf(numFmt.format(number));
    }

    public Float calculatePrice(Integer duration) {
        Float priceNoVAT = roundAsRequired(priceVAT / 1.21f);
        if (Zones.isMessagingService(serviceType))
            return priceNoVAT;
        Float pricePerSec = priceNoVAT / 60;
        Float price = pricePerSec * getAdjustedDuration(duration);
        log.debug(String.format("[calc price %s, %s, %s->%s, %s\n",
                price, pricePerSec, duration,
                getAdjustedDuration(duration),
                price));
        return roundAsRequired(price);
    }

    private Integer getAdjustedDuration(Integer duration) {
        String[] items = tarification.split("\\+");
        Integer firstMin = Integer.valueOf(items[0].trim());
        Integer otherMin = Integer.valueOf(items[1].trim());

        if (duration <= 60) {
            return adjustLessThanMinuteDuration(duration, firstMin);
        }
        Integer minuteRemainder = duration % 60;
//        System.out.printf("[adjust %s,%s\n",
//                minuteRemainder, duration);
        return duration - minuteRemainder + adjustLessThanMinuteDuration(
                minuteRemainder, otherMin);
    }

    private Integer adjustLessThanMinuteDuration(
            Integer duration, Integer minuteTarific) {
        if (duration <= 0)
            return 0;
        switch (minuteTarific) {
            case 60:
                return 60;
            case 30:
                if (duration > 30)
                    return 60;
                return 30;
            case 1:
                return duration;
            default:
                System.out.println("Warning: tarification not supported!!");
                return 999; //indicate not supported value
        }
    }
}
