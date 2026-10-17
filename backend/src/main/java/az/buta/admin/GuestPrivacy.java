package az.buta.admin;

import az.buta.auth.GuestAccounts;
import az.buta.common.web.PageResponse;
import az.buta.security.AuthenticatedUser;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GuestPrivacy {

    private final GuestAccounts guests;

    public GuestPrivacy(GuestAccounts guests) {
        this.guests = guests;
    }

    public PageResponse<AdminDtos.UserView> users(AuthenticatedUser viewer, PageResponse<AdminDtos.UserView> page) {
        if (!guests.isGuest(viewer)) {
            return page;
        }
        return new PageResponse<>(page.items().stream().map(AdminDtos.UserView::withMaskedEmail).toList(), page.page(), page.size(),
                page.totalItems(), page.totalPages());
    }

    public AdminDtos.UserView user(AuthenticatedUser viewer, AdminDtos.UserView view) {
        return guests.isGuest(viewer) ? view.withMaskedEmail() : view;
    }

    public List<AdminDtos.ReferralView> referrals(AuthenticatedUser viewer, List<AdminDtos.ReferralView> referrals) {
        return guests.isGuest(viewer) ? referrals.stream().map(AdminDtos.ReferralView::withMaskedEmails).toList() : referrals;
    }
}
