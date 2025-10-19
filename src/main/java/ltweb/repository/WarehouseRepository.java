package ltweb.repository;

import ltweb.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {
    
    Optional<Warehouse> findByCode(String code);
    
    Optional<Warehouse> findByUserId(Long userId);
    
    boolean existsByCode(String code);
}