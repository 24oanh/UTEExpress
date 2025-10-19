package ltweb.repository;

import ltweb.entity.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RouteRepository extends JpaRepository<Route, Long> {

	List<Route> findByFromWarehouseId(Long fromWarehouseId);

	List<Route> findByToWarehouseId(Long toWarehouseId);

	Optional<Route> findByFromWarehouseIdAndToWarehouseId(Long fromWarehouseId, Long toWarehouseId);

	List<Route> findByPreferredShipperId(Long shipperId);

	@Query("SELECT r FROM Route r WHERE r.fromWarehouse.id = :fromId AND r.toWarehouse.id = :toId AND r.isActive = true")
	Optional<Route> findActiveRoute(@Param("fromId") Long fromWarehouseId, @Param("toId") Long toWarehouseId);
}