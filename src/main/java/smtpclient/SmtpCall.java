package smtpclient;

import javax.activation.DataHandler;
import javax.mail.*;
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
import static javax.mail.Session.getDefaultInstance;
import static smtpclient.LookupMX.lookupMailHosts;
import static smtpclient.LookupMX.toMailServers;

public class SmtpCall {

    public static SmtpCall newSmtpCall() {
        return new SmtpCall();
    }

    private List<String> mailServers = new ArrayList<>();
    private Address sender;
    private InternetAddress recipient;
    private String subject;
    private Multipart content = new MimeMultipart();


    public SmtpCall mailserver(final String server) {
        this.mailServers.add(server);
        return this;
    }
    public SmtpCall sender(final Address sender) {
        this.sender = sender;
        return this;
    }

    /**
     * The address is assumed to be a syntactically valid RFC822 address.
     */
    public SmtpCall sender(final String name, final String emailAddress) {
        try {
            this.sender = new InternetAddress(emailAddress, name);
        } catch (final UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        return this;
    }
    public SmtpCall recipient(final InternetAddress recipient) {
        this.recipient = recipient;
        return this;
    }
    /**
     * The address is assumed to be a syntactically valid RFC822 address.
     */
    public SmtpCall recipient(final String name, final String emailAddress) {
        try {
            this.recipient = new InternetAddress(emailAddress, name);
        } catch (final UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        return this;
    }
    public SmtpCall subject(final String subject) {
        this.subject = subject;
        return this;
    }
    public SmtpCall addBodyText(final String message, final Charset charset) throws MessagingException {
        final MimeBodyPart textBodyPart = new MimeBodyPart();
        textBodyPart.setText(message, charset.toString());
        content.addBodyPart(textBodyPart);
        return this;
    }
    public SmtpCall addBodyHtml(final String message, final Charset charset) throws MessagingException {
        final MimeBodyPart htmlBodyPart = new MimeBodyPart();
        htmlBodyPart.setContent(message, "text/html; charset="+charset.name());
        content.addBodyPart(htmlBodyPart);
        return this;
    }
    public SmtpCall addAttachment(final String name, final String type, final byte[] data) throws MessagingException {
        final MimeBodyPart attachment = new MimeBodyPart();
        attachment.setDataHandler(new DataHandler(new ByteArrayDataSource(data, type)));
        attachment.setFileName(name);
        content.addBodyPart(attachment);
        return this;
    }



    public void send() throws NamingException, IOException {
        final var servers = !mailServers.isEmpty() ? mailServers : toMailServers(lookupMailHosts(recipient));
        for (final String mailServer : servers) {
            if (deliver(mailServer)) return;
        }
        throw new IOException("Failed to deliver email to any mail server");
    }

    private boolean deliver(final String mailServer) {
        try {
            final Session mailSession = getDefaultInstance(createMailSessionProperties(mailServer));
            final MimeMessage message = createMessage(mailSession, sender, subject, content, recipient);
            final Transport transport = mailSession.getTransport();
            try {
                transport.connect();
                transport.sendMessage(message, message.getRecipients(TO));
            } finally {
                transport.close();
            }
            return true;
        } catch (Exception e) {
            return false;
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

}
