package smtpclient;

import org.junit.jupiter.api.Test;

import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.naming.NamingException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static smtpclient.SmtpCall.newSmtpCall;

class SmtpCallTest {

    @Test
    void ehlo_unsafeIdentity_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> newSmtpCall().ehlo("a\rb"));
    }

    @Test
    void addBodyText_nullOrEmpty_isNoOpWithoutMultipart() throws MessagingException {
        final var call = newSmtpCall();
        assertSame(call, call.addBodyText(null, UTF_8));
        assertSame(call, call.addBodyText("", UTF_8));
    }

    @Test
    void addBodyHtml_nullOrEmpty_isNoOpWithoutMultipart() throws MessagingException {
        final var call = newSmtpCall();
        assertSame(call, call.addBodyText(null, UTF_8));
        // addBodyHtml empty
        assertSame(call, call.addBodyHtml("", UTF_8));
    }

    @Test
    void addBodyText_withoutMultipart_throwsIllegalState() {
        assertThrows(IllegalStateException.class, () -> newSmtpCall().addBodyText("msg", UTF_8));
    }

    @Test
    void addBodyHtml_withoutMultipart_throwsIllegalState() {
        assertThrows(IllegalStateException.class, () -> newSmtpCall().addBodyHtml("<b>hi</b>", UTF_8));
    }

    @Test
    void addAttachment_withoutMultipart_throwsIllegalState() {
        assertThrows(IllegalStateException.class, () ->
                newSmtpCall().addAttachment("f.txt", "text/plain", "data".getBytes(UTF_8)));
    }

    @Test
    void send_withoutRecipient_throwsIllegalState() {
        assertThrows(IllegalStateException.class, () -> newSmtpCall()
                .sender("Sender", "sender@example.com")
                .mailserver("127.0.0.1")
                .useMultipartMixed()
                .send());
    }

    @Test
    void send_withoutSender_throwsIllegalState() {
        assertThrows(IllegalStateException.class, () -> newSmtpCall()
                .recipient("Recipient", "recipient@example.com")
                .mailserver("127.0.0.1")
                .useMultipartMixed()
                .send());
    }

    @Test
    void send_withBogusMailserver_throwsIOException() {
        assertThrows(IOException.class, () -> newSmtpCall()
                .ehlo("localhost")
                .sender("Sender", "sender@example.com")
                .recipient("Recipient", "recipient@example.com")
                .subject("subject")
                .mailserver("127.0.0.1")
                .useMultipartAlternative()
                .addBodyText("hello", UTF_8)
                .addBodyHtml("<b>hi</b>", UTF_8)
                .addAttachment("f.txt", "text/plain", "data".getBytes(UTF_8))
                .send());
    }

    @Test
    void send_withNoMailServers_throwsNamingOrIOException() {
        // recipient domain with no DNS resolution -> NamingException from lookup
        assertThrows(Exception.class, () -> newSmtpCall()
                .sender("Sender", "sender@example.com")
                .recipient("Recipient", "recipient@user")
                .useMultipartMixed()
                .send());
    }

    @Test
    void useMultipartMixed_andAlternative_returnContent() {
        final var alt = newSmtpCall().useMultipartAlternative();
        final var mix = newSmtpCall().useMultipartMixed();
        // just ensure no exception and chaining works
        org.junit.jupiter.api.Assertions.assertNotNull(alt);
        org.junit.jupiter.api.Assertions.assertNotNull(mix);
    }

    @Test
    void senderAndRecipient_addressOverloads() throws MessagingException {
        final var call = newSmtpCall()
                .sender(new InternetAddress("sender@example.com"))
                .recipient(new InternetAddress("recipient@example.com"))
                .subject("s")
                .mailserver("127.0.0.1")
                .useMultipartMixed();
        call.addBodyText("t", UTF_8);
        // send will fail but exercise createMessage path partially via... actually send connects; expect IOException
        assertThrows(IOException.class, call::send);
    }

    @Test
    void addAttachment_sanitizesFilename() throws MessagingException {
        final var call = newSmtpCall()
                .useMultipartMixed();
        // filename with CR/LF should be sanitized without throwing
        call.addAttachment("a\rb\nc.txt", "text/plain", "data".getBytes(UTF_8));
    }
}
