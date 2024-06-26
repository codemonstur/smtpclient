package smtpclient;

import javax.mail.MessagingException;
import javax.naming.NamingException;
import java.io.IOException;
import java.nio.charset.Charset;

public interface SmtpCallContent {

    SmtpCallContent addBodyText(String message, Charset charset) throws MessagingException;
    SmtpCallContent addBodyHtml(String message, Charset charset) throws MessagingException;
    SmtpCallContent addAttachment(String name, String type, byte[] data) throws MessagingException;
    void send() throws NamingException, IOException;

}
