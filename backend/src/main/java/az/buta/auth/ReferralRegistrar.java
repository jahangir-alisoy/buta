package az.buta.auth;

import az.buta.user.domain.User;

public interface ReferralRegistrar {

    void register(User referee, String referralCode);
}
