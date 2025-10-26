package ltweb.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ltweb.entity.PackageConfirmation;

@Repository
public interface PackageConfirmationRepository extends JpaRepository<PackageConfirmation, Long> {
    List<PackageConfirmation> findByOrderId(Long orderId);
    List<PackageConfirmation> findByOrderIdAndIsConfirmedFalse(Long orderId);
    long countByOrderIdAndIsConfirmedFalse(Long orderId);
}
