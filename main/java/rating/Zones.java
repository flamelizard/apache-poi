package rating;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static rating.RatingQuick.getExcelSheetAsCsv;

public class Zones {
    public static final Pattern messagingPtrn = Pattern.compile("(SMS|MMS)", Pattern.CASE_INSENSITIVE);
    private String zoneFile;
    private String sheetName;
    private Logger log = Logger.getLogger(Zones.class.getName());
    private List<RatedZone> availableZones = new ArrayList<>();

    public Zones(String zoneFile, String sheetName) throws IOException {
        this.zoneFile = zoneFile;
        this.sheetName = sheetName;
        log.setLevel(Level.DEBUG);
        loadZoneFile();
    }

    public static String normalize(String zone) {
        String[] items = zone.toUpperCase().split("_");
        if (items.length < 2)
            return zone;
        String zoneId = items[1];
        if (zoneId.length() < 2) {
            items[1] = String.format("0%s", zoneId);
        }
        zone = items[0] + "_" + items[1];
        return zone.replace("D_", "DOMESTIC_").replace("F_", "FOREIGN_");
    }

    public static boolean isMessagingService(String name) {
        return messagingPtrn.matcher(name).find();
    }

    public static boolean isForeignZone(String zone) {
        return zone.toUpperCase().contains("FOREIGN");
    }

    private void loadZoneFile() throws IOException {
        Path path = Paths.get(zoneFile);
        List<String> csv = getExcelSheetAsCsv(path, sheetName);
//        data columns have no fixed position, use RE
        Pattern zoneItems = Pattern.compile(
                "([a-z]+_[0-9]{2}).+?([0-9]+\\+[0-9]+).+?([0-9.,]+) ?(kč|kc)",
                Pattern.CASE_INSENSITIVE);

        for (String items : csv) {
            Matcher match = zoneItems.matcher(items);
            if (match.find()) {
//                System.out.printf("[parse %s,%s,%s\n",
//                        match.group(1), match.group(2), match.group(3));
                availableZones.add(
                        new RatedZone(match.group(1), match.group(2),
                                match.group(3), getServiceType(items))
                );
            }
        }
        if (availableZones.isEmpty())
            throw new RatingException("Missing rating hints in Rating excel");
//        availableZones.forEach((r)-> System.out.println("[zone " + r));
    }

    public RatedZone getRatedZoneOrGeneric(String zone) {
        return getRatedZoneOrGeneric(zone, "");
    }

    public RatedZone getRatedZoneOrGeneric(String zone, String service) {
        if (zone.equals(""))
            zone = "GENERIC";
        if (isMessagingService(service) && isForeignZone(zone))
            zone = "FOREIGN_13";    // SMS/MMS foreign is always this zone
        for (RatedZone rated : availableZones) {
            if (rated.getZone().equals(zone) &&
                    rated.getService().equals(getServiceType(service))) {
                return rated;
            }
        }
        return new RatedZone("GENERIC", "60+60", "0");
    }

    private String getServiceType(String name) {
        Matcher match = messagingPtrn.matcher(name);
        if (match.find())
            return match.group(1).toUpperCase();
        return "N/A";
    }
}
