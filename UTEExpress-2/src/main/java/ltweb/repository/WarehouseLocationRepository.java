package ltweb.repository;

import ltweb.entity.LocationStatus;
import ltweb.entity.WarehouseLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseLocationRepository extends JpaRepository<WarehouseLocation, Long> {
    
    List<WarehouseLocation> findByWarehouseId(Long warehouseId);
    
    List<WarehouseLocation> findByWarehouseIdAndStatus(Long warehouseId, LocationStatus status);
    
    Optional<WarehouseLocation> findByLocationCode(String locationCode);
    
    boolean existsByLocationCode(String locationCode);
}