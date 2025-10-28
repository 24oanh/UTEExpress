package ltweb.repository;

import ltweb.entity.CustomerAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, Long> {
    
    List<CustomerAddress> findByCustomerIdOrderByIsDefaultDescCreatedAtDesc(Long customerId);
    
    Optional<CustomerAddress> findByCustomerIdAndIsDefault(Long customerId, Boolean isDefault);
    
    long countByCustomerId(Long customerId);
    
    boolean existsByIdAndCustomerId(Long id, Long customerId);
}