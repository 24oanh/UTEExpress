package ltweb.repository;

import ltweb.entity.VerificationToken;
import ltweb.entity.VerificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
    
    Optional<VerificationToken> findByToken(String token);
    
    Optional<VerificationToken> findByTokenAndType(String token, VerificationType type);
    
    List<VerificationToken> findByUserIdAndType(Long userId, VerificationType type);
    
    void deleteByExpiryDateBefore(LocalDateTime dateTime);
    
    void deleteByUserId(Long userId);
}