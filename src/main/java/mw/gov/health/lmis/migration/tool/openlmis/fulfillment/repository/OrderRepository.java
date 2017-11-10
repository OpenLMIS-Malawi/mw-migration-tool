package mw.gov.health.lmis.migration.tool.openlmis.fulfillment.repository;

import mw.gov.health.lmis.migration.tool.openlmis.fulfillment.domain.Order;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends CrudRepository<Order, UUID> {

  Order findByExternalId(UUID externalId);
  
}
