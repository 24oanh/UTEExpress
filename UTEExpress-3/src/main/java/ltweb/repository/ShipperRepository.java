package ltweb.repository;

import ltweb.entity.Shipper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShipperRepository extends JpaRepository<Shipper, Long> {
    
    Optional<Shipper> findByCode(String code);
    
    Optional<Shipper> findByUserId(Long userId);
    
    List<Shipper> findByIsActive(Boolean isActive);
    
    boolean existsByCode(String code);
    
    boolean existsByEmail(String email);
    
    boolean existsByPhone(String phone);
}