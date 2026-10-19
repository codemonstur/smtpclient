package smtpclient;

import javax.mail.internet.InternetAddress;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.InitialDirContext;
import java.net.IDN;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

public enum LookupMX {;

    public static List<MXRecord> lookupMailHosts(final InternetAddress email) throws NamingException {
        final var address = email.getAddress();
        final var domainName = extractDomain(address);
        if (domainName == null || domainName.isEmpty()) return emptyList();

        // a domain literal can not be looked up in dns: deliver to the address itself
        if (isDomainLiteral(address))
            return List.of(new MXRecord(domainName, 0));

        final var attributeMX = getMXrecordsForDomain(domainName);
        // no MX records: rfc 5321 5.1 falls back to the domain's A record
        if (attributeMX == null) return lookupAddressRecords(domainName);

        final var records = new ArrayList<MXRecord>(attributeMX.size());
        for (int i = 0; i < attributeMX.size(); i++) {
            final var record = new MXRecord(attributeMX.get(i).toString().split("\\s+"));
            // rfc 7505: a null MX ("0 .") means the domain explicitly accepts no email
            if (record.hostname.isEmpty()) return emptyList();
            records.add(record);
        }
        Collections.sort(records);
        return records;
    }

    public static List<String> toMailServers(final List<MXRecord> records) {
        return records.stream().map(mxRecord -> mxRecord.hostname).collect(toList());
    }

    private static String extractDomain(final String address) {
        // lastIndexOf: the local part may legally contain an '@' inside quotes
        final int separator = address.lastIndexOf('@');
        var domain = separator == -1 ? address : address.substring(separator + 1);
        if (isDomainLiteral(address)) domain = domain.substring(1, domain.length() - 1);
        try {
            return IDN.toASCII(domain);
        } catch (final IllegalArgumentException e) {
            return domain;
        }
    }

    private static boolean isDomainLiteral(final String address) {
        final int separator = address.lastIndexOf('@');
        final var domain = separator == -1 ? address : address.substring(separator + 1);
        return domain.length() > 2 && domain.startsWith("[") && domain.endsWith("]");
    }

    private static final String[] MX_RECORD = { "MX" };
    private static Attribute getMXrecordsForDomain(final String domainName) throws NamingException {
        final var context = new InitialDirContext();
        try {
            return context.getAttributes("dns:/" + domainName, MX_RECORD).get("MX");
        } finally {
            try { context.close(); } catch (final NamingException ignored) { }
        }
    }

    private static final String[] A_RECORD = { "A" };
    private static List<MXRecord> lookupAddressRecords(final String domainName) {
        InitialDirContext context = null;
        try {
            context = new InitialDirContext();
            final var addresses = context.getAttributes("dns:/" + domainName, A_RECORD).get("A");
            if (addresses == null || addresses.size() == 0) return emptyList();
            return List.of(new MXRecord(domainName, 0));
        } catch (final NamingException e) {
            return emptyList();
        } finally {
            if (context != null) try { context.close(); } catch (final NamingException ignored) { }
        }
    }

    public static class MXRecord implements Comparable<MXRecord> {
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

        @Override
        public String toString() {
            return hostname;
        }
    }

}
