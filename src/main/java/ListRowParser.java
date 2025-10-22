public final class ListRowParser {
    private ListRowParser() {}

    public static String extractUrlFromRow(String row) {
        int idx = row.indexOf("->");
        if (idx >= 0) {
            String tail = row.substring(idx + 2).trim();
            return tail.split("\\s+")[0];
        }
        return row.trim();
    }

    public static String extractNameFromRow(String row) {
        int idx = row.indexOf("->");
        if (idx >= 0) {
            return row.substring(0, idx).trim();
        }
        return row.trim();
    }

    public static String extractIpFromRow(String row) {
        String ipRegex = "\\b(\\d{1,3}\\.){3}\\d{1,3}\\b";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(ipRegex).matcher(row);
        if (m.find()) {
            return m.group();
        }
        return "";
    }
}