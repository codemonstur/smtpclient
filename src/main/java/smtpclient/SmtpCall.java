package smtpclient;

import javax.activation.DataHandler;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import javax.naming.NamingException;
import java.io.EOFException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static javax.mail.Message.RecipientType.TO;
import static javax.mail.Part.ATTACHMENT;
import static javax.mail.Session.getDefaultInstance;
import static smtpclient.LookupMX.lookupMailHosts;
import static smtpclient.LookupMX.toMailServers;

public class SmtpCall implements SmtpCallHeaders, SmtpCallContent {

    public static SmtpCall newSmtpCall() {
        return new SmtpCall();
    }

    private List<String> mailServers = new ArrayList<>();
    private Address sender;
    private InternetAddress recipient;
    private String subject;
    private Multipart content;

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
            this.sender = new InternetAddress(emailAddress, name);
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
            this.recipient = new InternetAddress(emailAddress, name);
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

        final MimeBodyPart textBodyPart = new MimeBodyPart();
        textBodyPart.setText(message, "text/plain; charset=" + charset.toString());
        content.addBodyPart(textBodyPart);
        return this;
    }
    public SmtpCallContent addBodyHtml(final String message, final Charset charset) throws MessagingException {
        if (isNullOrEmpty(message)) return this;

        final MimeBodyPart htmlBodyPart = new MimeBodyPart();
        htmlBodyPart.setContent(message, "text/html; charset=" + charset.name());
        content.addBodyPart(htmlBodyPart);
        return this;
    }
    public SmtpCallContent addAttachment(final String name, final String type, final byte[] data) throws MessagingException {
        final MimeBodyPart attachment = new MimeBodyPart();
        attachment.setDataHandler(new DataHandler(new ByteArrayDataSource(data, type)));
        attachment.setFileName(name);
        attachment.setDisposition(ATTACHMENT);
        content.addBodyPart(attachment);
        return this;
    }


    public void send() throws NamingException, IOException {
        if (mailServers.isEmpty()) mailServers = toMailServers(lookupMailHosts(recipient));
        final var causes = new ArrayList<String>();
        for (final String mailServer : mailServers) {
            try {
                deliverReturnError(mailServer);
            } catch (final Exception e) {
                causes.add("Mailserver " + mailServer + " failed to deliver for these reasons:\n" + toFullMessage(e));
            }
        }
        throw new IOException("Failed to deliver email to address " + recipient + "\nCauses: " + causes);
    }

    private static String toFullMessage(final Throwable e) {
        final String message = e.getClass().getSimpleName() + ": " + e.getMessage();
        return e.getCause() == null ? message : message + "\n" + toFullMessage(e.getCause());
    }

    private void deliverReturnError(final String mailServer) throws MessagingException {
        final Session mailSession = getDefaultInstance(createMailSessionProperties(mailServer));
        final MimeMessage message = createMessage(mailSession, sender, subject, content, recipient);
        final Transport transport = mailSession.getTransport();
        try {
            transport.connect();
            transport.sendMessage(message, message.getRecipients(TO));
        } finally {
            transport.close();
        }
    }

    private static Properties createMailSessionProperties(final String mailServer) {
        final Properties props = new Properties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.host", mailServer);
        props.put("mail.smtp.auth", "false");
        return props;
    }
    private static MimeMessage createMessage(final Session mailSession, final Address sender, final String subject,
                                             final Multipart content, final InternetAddress recipient) throws MessagingException {
        final MimeMessage message = new MimeMessage(mailSession);
        message.setFrom(sender);
        message.setSubject(subject);
        message.setContent(content);
        message.addRecipient(TO, recipient);
        message.saveChanges();
        return message;
    }

    private static boolean isNullOrEmpty(final String value) {
        return value == null || value.isEmpty();
    }

}
