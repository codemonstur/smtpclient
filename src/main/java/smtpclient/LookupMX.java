package smtpclient;

import javax.mail.internet.InternetAddress;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.InitialDirContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

public enum LookupMX {;

    public static List<MXRecord> lookupMailHosts(final InternetAddress email) throws NamingException {
        final var domainName = email.getAddress().substring(email.getAddress().indexOf('@') + 1);
        final var attributeMX = getMXrecordsForDomain(domainName);
        if (attributeMX == null) return emptyList();

        final var records = new ArrayList<MXRecord>(attributeMX.size());
        for (int i = 0; i < attributeMX.size(); i++) {
            records.add(new MXRecord(attributeMX.get(i).toString().split("\\s+")));
        }
        Collections.sort(records);
        return records;
    }

    public static List<String> toMailServers(final List<MXRecord> records) {
        return records.stream().map(mxRecord -> mxRecord.hostname).collect(toList());
    }


    private static final String[] MX_RECORD = { "MX" };
    private static Attribute getMXrecordsForDomain(final String domainName) throws NamingException {
        return new InitialDirContext().getAttributes("dns:/" + domainName, MX_RECORD).get("MX");
    }

    private static class MXRecord implements Comparable<MXRecord> {
        private final String hostname;
        private final Integer preference;

        private MXRecord(final String[] line) {
            this(line[1], Integer.parseInt(line[0]));
        }
        private MXRecord(final String hostname, final Integer preference) {
            this.hostname = hostname.endsWith(".") ? hostname.substring(0, hostname.length()-1) : hostname;
            this.preference = preference;
        }

        @Override
        public int compareTo(final MXRecord o) {
            return preference.compareTo(o.preference);
        }
    }

}
