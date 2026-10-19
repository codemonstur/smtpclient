package smtpclient;

import javax.activation.DataHandler;
import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import javax.naming.NamingException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static javax.mail.Message.RecipientType.TO;
import static javax.mail.Part.ATTACHMENT;
import static javax.mail.Session.getInstance;
import static smtpclient.LookupMX.lookupMailHosts;
import static smtpclient.LookupMX.toMailServers;

public class SmtpCall implements SmtpCallHeaders, SmtpCallContent {

    public static SmtpCall newSmtpCall() {
        return new SmtpCall();
    }

    private String identity;
    private List<String> mailServers = new ArrayList<>();
    private Address sender;
    private InternetAddress recipient;
    private String subject;
    private Multipart content;

    public SmtpCallHeaders ehlo(final String identity) {
        // the identity is sent verbatim inside the EHLO command
        checkSafeForSmtpCommand(identity, "EHLO identity");
        this.identity = identity;
        return this;
    }
    public SmtpCallHeaders mailserver(final String server) {
        this.mailServers.add(server);
        return this;
    }
    public SmtpCallHeaders sender(final Address sender) {
        this.sender = sender;
        return this;
    }

    /**
     * The address is assumed to be a syntactically valid RFC822 address.
     */
    public SmtpCallHeaders sender(final String name, final String emailAddress) {
        try {
            this.sender = new InternetAddress(emailAddress, name, "UTF-8");
        } catch (final UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        return this;
    }
    public SmtpCallHeaders recipient(final InternetAddress recipient) {
        this.recipient = recipient;
        return this;
    }
    /**
     * The address is assumed to be a syntactically valid RFC822 address.
     */
    public SmtpCallHeaders recipient(final String name, final String emailAddress) {
        try {
            this.recipient = new InternetAddress(emailAddress, name, "UTF-8");
        } catch (final UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        return this;
    }
    public SmtpCallHeaders subject(final String subject) {
        this.subject = subject;
        return this;
    }

    @Override
    public SmtpCallContent useMultipartAlternative() {
        this.content = new MimeMultipart("alternative");
        return this;
    }

    @Override
    public SmtpCallContent useMultipartMixed() {
        this.content = new MimeMultipart("mixed");
        return this;
    }

    public SmtpCallContent addBodyText(final String message, final Charset charset) throws MessagingException {
        if (isNullOrEmpty(message)) return this;
        requireMultipart();

        final MimeBodyPart textBodyPart = new MimeBodyPart();
        textBodyPart.setText(message, charset.name());
        content.addBodyPart(textBodyPart);
        return this;
    }
    public SmtpCallContent addBodyHtml(final String message, final Charset charset) throws MessagingException {
        if (isNullOrEmpty(message)) return this;
        requireMultipart();

        final MimeBodyPart htmlBodyPart = new MimeBodyPart();
        htmlBodyPart.setContent(message, "text/html; charset=" + charset.name());
        content.addBodyPart(htmlBodyPart);
        return this;
    }
    public SmtpCallContent addAttachment(final String name, final String type, final byte[] data) throws MessagingException {
        requireMultipart();

        final MimeBodyPart attachment = new MimeBodyPart();
        attachment.setDataHandler(new DataHandler(new ByteArrayDataSource(data, type)));
        // the filename ends up verbatim in the Content-Disposition header of the DATA
        attachment.setFileName(name.replace('\r', ' ').replace('\n', ' '));
        attachment.setDisposition(ATTACHMENT);
        content.addBodyPart(attachment);
        return this;
    }

    private void requireMultipart() {
        if (content == null)
            throw new IllegalStateException("Call useMultipartAlternative() or useMultipartMixed() before adding content");
    }


    public void send() throws NamingException, IOException {
        if (recipient == null) throw new IllegalStateException("No recipient has been set");
        if (sender == null) throw new IllegalStateException("No sender has been set");
        // the addresses end up verbatim inside smtp commands: CR/LF or angle
        // brackets would let a caller forge extra envelope commands
        checkSafeForSmtpCommand(recipient.getAddress(), "recipient address");
        checkSafeForSmtpCommand(toEnvelopeAddress(sender), "sender address");

        if (mailServers.isEmpty()) mailServers = toMailServers(lookupMailHosts(recipient));
        if (mailServers.isEmpty())
            throw new IOException("No mail servers found for " + recipient.getAddress());

        final var causes = new ArrayList<String>();
        for (final String mailServer : mailServers) {
            try {
                deliverReturnError(mailServer);
                return;
            } catch (final Exception e) {
                causes.add("Mailserver " + mailServer + " failed to deliver for these reasons:\n" + toFullMessage(e));
            }
        }
        throw new IOException("Failed to deliver email to address " + recipient + "\nCauses: " + causes);
    }

    private static String toEnvelopeAddress(final Address address) {
        return address instanceof InternetAddress ? ((InternetAddress) address).getAddress() : address.toString();
    }

    private static void checkSafeForSmtpCommand(final String value, final String what) {
        if (value == null) return;
        if (value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0
                || value.indexOf('<') >= 0 || value.indexOf('>') >= 0)
            throw new IllegalArgumentException("The " + what
                    + " contains characters that are not allowed in an SMTP command: " + value);
    }

    private static String toFullMessage(final Throwable e) {
        final String message = e.getClass().getSimpleName() + ": " + e.getMessage();
        return e.getCause() == null ? message : message + "\n" + toFullMessage(e.getCause());
    }

    private void deliverReturnError(final String mailServer) throws MessagingException {
        // getInstance: getDefaultInstance caches the first session JVM-wide and would
        // silently ignore the properties of every later send (breaking MX failover)
        final var mailSession = getInstance(createMailSessionProperties(identity, mailServer));
        final var message = createMessage(mailSession, sender, subject, content, recipient);
        try (final var transport = mailSession.getTransport()) {
            transport.connect();
            transport.sendMessage(message, message.getRecipients(TO));
        }
    }

    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 60_000;
    private static final int WRITE_TIMEOUT_MS = 30_000;

    private static Properties createMailSessionProperties(final String identity, final String destination) {
        final var props = new Properties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.host", destination);
        props.put("mail.smtp.auth", "false");
        // encrypt whenever the server offers it, and verify the server identity when it does
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.ssl.checkserveridentity", "true");
        // without timeouts an unresponsive server blocks the calling thread forever
        props.put("mail.smtp.connectiontimeout", String.valueOf(CONNECT_TIMEOUT_MS));
        props.put("mail.smtp.timeout", String.valueOf(READ_TIMEOUT_MS));
        props.put("mail.smtp.writetimeout", String.valueOf(WRITE_TIMEOUT_MS));
        if (!isNullOrEmpty(identity))
            props.put("mail.smtp.localhost", identity);
        return props;
    }
    private static MimeMessage createMessage(final Session mailSession, final Address sender, final String subject,
                                             final Multipart content, final InternetAddress recipient) throws MessagingException {
        final var message = new MimeMessage(mailSession);
        message.setFrom(sender);
        // explicit charset: the platform default would garble non-ascii subjects
        message.setSubject(subject, "UTF-8");
        message.setContent(content);
        message.addRecipient(TO, recipient);
        message.saveChanges();
        return message;
    }

    private static boolean isNullOrEmpty(final String value) {
        return value == null || value.isEmpty();
    }

}
