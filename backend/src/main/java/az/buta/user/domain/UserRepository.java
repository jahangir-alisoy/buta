package az.buta.user.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByReferralCode(String referralCode);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByReferralCode(String referralCode);

    @Query("select u from User u where lower(u.email) like :query or lower(coalesce(u.username, '')) like :query "
            + "or lower(coalesce(u.firstName, '')) like :query or lower(coalesce(u.lastName, '')) like :query")
    Page<User> search(@Param("query") String query, Pageable pageable);
}
