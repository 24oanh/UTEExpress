package ltweb.repository;

import ltweb.entity.Package;
import ltweb.entity.PackageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PackageRepository extends JpaRepository<Package, Long> {
    
    Optional<Package> findByPackageCode(String packageCode);
    
    List<Package> findByOrderId(Long orderId);
    
    List<Package> findByStatus(PackageStatus status);
    
    boolean existsByPackageCode(String packageCode);
}