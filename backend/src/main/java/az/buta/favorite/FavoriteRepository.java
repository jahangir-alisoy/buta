package az.buta.favorite;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface FavoriteRepository extends JpaRepository<Favorite, Favorite.Key> {

    @Query("select f from Favorite f where f.key.userId = :userId order by f.createdAt desc")
    List<Favorite> findByUser(@Param("userId") UUID userId);
}
