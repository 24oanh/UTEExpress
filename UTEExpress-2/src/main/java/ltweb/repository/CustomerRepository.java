package ltweb.repository;

import ltweb.entity.Customer;
import ltweb.entity.CustomerStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    
    Optional<Customer> findByCustomerCode(String customerCode);
    
    Optional<Customer> findByEmail(String email);
    
    Optional<Customer> findByPhone(String phone);
    
    Optional<Customer> findByUserId(Long userId);
    
    Optional<Customer> findByEmailOrPhone(String email, String phone);
    
    boolean existsByEmail(String email);
    
    boolean existsByPhone(String phone);
    
    boolean existsByCustomerCode(String customerCode);
    
    long countByStatus(CustomerStatus status);
}