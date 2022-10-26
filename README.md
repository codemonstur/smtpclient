
[![GitHub Release](https://img.shields.io/github/release/codemonstur/smtpclient.svg)](https://github.com/codemonstur/slate4j-bobplugin/releases) 
[![MIT Licence](https://badges.frapsoft.com/os/mit/mit.svg?v=103)](https://opensource.org/licenses/mit-license.php)

# An SMTP client

Characteristics:
- Single dependency: javax.mail
- Pure Java
- Can deliver email directly to the Mail Exchanger of the recipient
  - So you dont need to configure a mail server to send email
- Small

## Code Example

    newSmtpCall()
        .sender("John Doe", "johndoe@test.com")
        .recipient("Mary Jane", "mary-jane@gmail.com")
        .subject("A test subject")
        .addBodyText("Just a test message", UTF_8)
        .send();
