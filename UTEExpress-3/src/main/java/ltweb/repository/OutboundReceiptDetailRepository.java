package ltweb.repository;

import ltweb.entity.OutboundReceiptDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboundReceiptDetailRepository extends JpaRepository<OutboundReceiptDetail, Long> {
    
    List<OutboundReceiptDetail> findByOutboundReceiptId(Long outboundReceiptId);
}