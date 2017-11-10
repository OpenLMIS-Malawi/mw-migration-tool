package mw.gov.health.lmis.migration.tool.openlmis.fulfillment.repository;

import org.springframework.data.repository.CrudRepository;

import mw.gov.health.lmis.migration.tool.openlmis.fulfillment.domain.Order;
import mw.gov.health.lmis.migration.tool.openlmis.fulfillment.domain.ProofOfDelivery;

import java.util.UUID;

public interface ProofOfDeliveryRepository extends CrudRepository<ProofOfDelivery, UUID> {

  ProofOfDelivery findByOrder(Order order);

}
