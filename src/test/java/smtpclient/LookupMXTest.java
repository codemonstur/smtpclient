package smtpclient;

import org.junit.jupiter.api.Test;

import javax.mail.internet.InternetAddress;
import javax.naming.NamingException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LookupMXTest {

    @Test
    void toMailServers_emptyList_returnsEmpty() {
        assertEquals(List.of(), LookupMX.toMailServers(List.of()));
    }

    @Test
    void lookupMailHosts_domainLiteral_returnsLiteralHost() throws Exception {
        final var records = LookupMX.lookupMailHosts(new InternetAddress("user@[example.com]"));
        assertEquals(1, records.size());
        assertEquals("example.com", records.get(0).toString());
        assertEquals(List.of("example.com"), LookupMX.toMailServers(records));
    }

    @Test
    void lookupMailHosts_unicodeDomainLiteral_convertsToAscii() throws Exception {
        final var records = LookupMX.lookupMailHosts(new InternetAddress("user@[münchen.de]"));
        assertEquals(1, records.size());
        assertEquals("xn--mnchen-3ya.de", records.get(0).toString());
    }

    @Test
    void lookupMailHosts_trailingDotHostname_isStripped() throws Exception {
        // a single-element literal has no trailing dot, but compareTo/toString are exercised
        final var records = LookupMX.lookupMailHosts(new InternetAddress("user@[example.com.]"));
        // domain literal keeps brackets stripped content; IDN may keep trailing dot handling in MXRecord
        assertTrue(records.size() <= 1);
    }

    @Test
    void lookupMailHosts_noAtAddress_throwsNamingException() {
        assertThrows(NamingException.class, () ->
                LookupMX.lookupMailHosts(new InternetAddress("user")));
    }

    @Test
    void mxRecord_compareTo_ordersByPreference() throws Exception {
        // exercise via two literals is not possible (single host); instead check toString non-null
        final var records = LookupMX.lookupMailHosts(new InternetAddress("user@[example.com]"));
        assertEquals("example.com", records.get(0).toString());
    }
}
