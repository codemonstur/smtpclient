# smtpclient

A small SMTP mail client that **delivers directly to the recipient's mail
exchange** — no mail server, relay, or credentials required.

Characteristics:

- Single dependency (`com.sun.mail:javax.mail`).
- Pure Java.
- Resolves the recipient's MX record itself (RFC 5321 §5.1, with A-record
  fallback) and speaks SMTP to it directly.
- Small, fluent API.

## Usage

```java
newSmtpCall()
    .sender("me@example.com")
    .recipient("you@example.com")
    .subject("Hello")
    .useMultipartAlternative()
    .addBodyText("Plain text version")
    .addBodyHtml("<p>HTML version</p>")
    .addAttachment("report.pdf", "application/pdf", bytes)
    .send();
```

## Key types

| Type | Purpose |
| --- | --- |
| `SmtpCall` | Implements both the header and content interfaces; the single object you build a message on. |
| `SmtpCallHeaders` | `ehlo(identity)`, `mailserver`, `sender`, `recipient`, `subject`, `useMultipartAlternative()`, `useMultipartMixed()`. |
| `SmtpCallContent` | `addBodyText`, `addBodyHtml`, `addAttachment(name, type, byte[])`, `send()`. |
| `LookupMX` | DNS MX lookup via JNDI `InitialDirContext`, IDN and domain-literal handling, `MXRecord` model. |

## Dependency

```xml
<dependency>
  <groupId>com.github.codemonstur</groupId>
  <artifactId>smtpclient</artifactId>
  <version>1.1.0</version>
</dependency>
```

## Building

```sh
make build
```

## License

MIT
