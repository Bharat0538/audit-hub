package in.audithub.iam.repository;

import in.audithub.iam.model.UserRole;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {
    List<UserRole> findByUserId(UUID userId);

    /**
     * Eagerly fetch the User association to avoid LazyInitializationException
     * when the Hibernate session is closed outside a @Transactional context.
     */
    @EntityGraph(attributePaths = {"user"})
    List<UserRole> findByOrganizationId(UUID organizationId);
}
