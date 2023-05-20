import smtpclient.LookupMX;
import smtpclient.LookupMX.MXRecord;

import javax.mail.internet.InternetAddress;
import javax.naming.NamingException;
import java.io.UnsupportedEncodingException;
import java.util.List;

public class LookupServers {

    public static void main(final String... args) throws UnsupportedEncodingException, NamingException {
        final var servers = LookupMX.lookupMailHosts(new InternetAddress("jegvoorneveld@gmail.com", "Jurgen"));
        System.out.println(servers);
    }

}
