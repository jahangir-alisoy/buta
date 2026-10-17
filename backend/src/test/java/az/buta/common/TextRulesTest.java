package az.buta.common;

import az.buta.common.text.SafeUrls;
import az.buta.common.text.SearchKeys;
import az.buta.user.domain.EmailAddresses;
import az.buta.user.domain.Usernames;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TextRulesTest {

    @Test
    void searchKeysFoldAzerbaijaniLetters() {
        assertThat(SearchKeys.fold("Bakı Səhər Gəzintisi")).isEqualTo("baki seher gezintisi");
        assertThat(SearchKeys.fold("İçərişəhər ÖÜÇĞ")).isEqualTo("iceriseher oucg");
        assertThat(SearchKeys.build("Xəzər", null, "  ", "Kino!")).isEqualTo("xezer kino");
    }

    @Test
    void emailsAreNormalizedAndCanonicalizedForAbuseChecks() {
        assertThat(EmailAddresses.normalize("  Kamran.Plus@Buta.TEST ")).isEqualTo("kamran.plus@buta.test");
        assertThat(EmailAddresses.isValid("kamran.plus@buta.test")).isTrue();
        assertThat(EmailAddresses.isValid("not-an-email")).isFalse();
        assertThat(EmailAddresses.canonical("j.o.h.n+promo@gmail.com")).isEqualTo("john@gmail.com");
        assertThat(EmailAddresses.canonical("j.o.h.n+promo@example.az")).isEqualTo("j.o.h.n@example.az");
        assertThat(EmailAddresses.mask("kamran@buta.test")).isEqualTo("k***@buta.test");
    }

    @Test
    void usernamesFollowFormatAndReservedRules() {
        assertThat(Usernames.isAllowed("aysel_m")).isTrue();
        assertThat(Usernames.isAllowed("ab")).isFalse();
        assertThat(Usernames.isAllowed("1abc")).isFalse();
        assertThat(Usernames.isAllowed("admin")).isFalse();
        assertThat(Usernames.normalize(" Leyla ")).isEqualTo("leyla");
    }

    @Test
    void onlySafeImageLocationsAreAccepted() {
        assertThat(SafeUrls.isSafeImagePath("/images/demo/cinema-1.svg")).isTrue();
        assertThat(SafeUrls.isSafeImagePath("https://cdn.example.az/a.png")).isTrue();
        assertThat(SafeUrls.isSafeImagePath("javascript:alert(1)")).isFalse();
        assertThat(SafeUrls.isSafeImagePath("/images/../secret")).isFalse();
        assertThat(SafeUrls.isHttps("http://insecure.example")).isFalse();
    }
}
