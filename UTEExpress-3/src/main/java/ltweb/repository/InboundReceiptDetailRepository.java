package ltweb.repository;

import ltweb.entity.InboundReceiptDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InboundReceiptDetailRepository extends JpaRepository<InboundReceiptDetail, Long> {
    
    List<InboundReceiptDetail> findByInboundReceiptId(Long inboundReceiptId);
}