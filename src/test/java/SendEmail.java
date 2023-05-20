import javax.mail.MessagingException;
import javax.naming.NamingException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static java.nio.charset.StandardCharsets.UTF_8;
import static smtpclient.SmtpCall.newSmtpCall;

public class SendEmail {

    public static void main(final String... args) throws MessagingException, NamingException, IOException {
        newSmtpCall()
            .sender("Jurgen Test", "no-reply@3rd-stage.nl")
            .recipient("Jurgen Voorneveld", "jegvoorneveld@gmail.com")
            .subject("A test subject")
            .addBodyText("Just a test message", UTF_8)
            .addBodyHtml("<html><body><b>html content</b></body></html>", UTF_8)
            .send();
    }

}
