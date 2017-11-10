package mw.gov.health.lmis.migration.tool.openlmis.fulfillment.repository;

import org.springframework.data.repository.CrudRepository;

import mw.gov.health.lmis.migration.tool.openlmis.fulfillment.domain.Order;

import java.util.UUID;

public interface OrderRepository extends CrudRepository<Order, UUID> {

  Order findByExternalId(UUID externalId);
  
}
