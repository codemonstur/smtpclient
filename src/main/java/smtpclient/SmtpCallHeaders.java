package smtpclient;

import javax.mail.Address;
import javax.mail.internet.InternetAddress;
import java.io.UnsupportedEncodingException;

public interface SmtpCallHeaders {

    SmtpCallHeaders mailserver(String server);
    SmtpCallHeaders sender(Address sender);

    /**
     * The address is assumed to be a syntactically valid RFC822 address.
     */
    SmtpCallHeaders sender(String name, String emailAddress);
    SmtpCallHeaders recipient(InternetAddress recipient);
    /**
     * The address is assumed to be a syntactically valid RFC822 address.
     */
    SmtpCallHeaders recipient(String name, String emailAddress);
    SmtpCallHeaders subject(String subject);

    SmtpCallContent useMultipartAlternative();
    SmtpCallContent useMultipartMixed();

}
