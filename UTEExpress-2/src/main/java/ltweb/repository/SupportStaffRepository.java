package ltweb.repository;

import ltweb.entity.SupportStaff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SupportStaffRepository extends JpaRepository<SupportStaff, Long> {
    Optional<SupportStaff> findByUserId(Long userId);
    List<SupportStaff> findByIsOnlineTrue();
    List<SupportStaff> findByIsActiveTrue();
}