package ltweb.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import ltweb.entity.PackageConfirmation;
import ltweb.entity.PackageStatus;
import ltweb.entity.User;
import ltweb.repository.PackageConfirmationRepository;
import ltweb.repository.PackageRepository;
import ltweb.entity.Package;

@Service
@RequiredArgsConstructor
public class PackageConfirmationService {
    private final PackageConfirmationRepository confirmationRepository;
    private final PackageRepository packageRepository;

    public PackageConfirmationRepository getConfirmationRepository() {
        return confirmationRepository;
    }

    @Transactional
    public PackageConfirmation createConfirmation(PackageConfirmation confirmation) {
        return confirmationRepository.save(confirmation);
    }

    public List<PackageConfirmation> getConfirmationsByOrder(Long orderId) {
        return confirmationRepository.findByOrderId(orderId);
    }

    public List<PackageConfirmation> getPendingConfirmations(Long orderId) {
        return confirmationRepository.findByOrderIdAndIsConfirmedFalse(orderId);
    }

    @Transactional
    public Package confirmPackage(Long confirmationId, User confirmedBy) {
        PackageConfirmation confirmation = confirmationRepository.findById(confirmationId)
                .orElseThrow(() -> new RuntimeException("Confirmation not found"));

        // Create actual package
        Package pkg = Package.builder()
                .packageCode(confirmation.getPackageCode())
                .order(confirmation.getOrder())
                .description(confirmation.getDescription())
                .weight(confirmation.getWeight())
                .length(confirmation.getLength())
                .width(confirmation.getWidth())
                .height(confirmation.getHeight())
                .unitQuantity(confirmation.getUnitQuantity())
                .status(PackageStatus.KHO)
                .build();

        pkg = packageRepository.save(pkg);

        // Mark confirmation as confirmed
        confirmation.setIsConfirmed(true);
        confirmation.setConfirmedBy(confirmedBy.getId());
        confirmation.setConfirmedAt(LocalDateTime.now());
        confirmationRepository.save(confirmation);

        return pkg;
    }

    @Transactional
    public void confirmAllPackages(Long orderId, User confirmedBy) {
        List<PackageConfirmation> pending = getPendingConfirmations(orderId);
        for (PackageConfirmation confirmation : pending) {
            confirmPackage(confirmation.getId(), confirmedBy);
        }
    }

    public boolean hasUnconfirmedPackages(Long orderId) {
        return confirmationRepository.countByOrderIdAndIsConfirmedFalse(orderId) > 0;
    }
}