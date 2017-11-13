package mw.gov.health.lmis.migration.tool.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import mw.gov.health.lmis.migration.tool.AppContext;
import mw.gov.health.lmis.migration.tool.config.ToolProperties;
import mw.gov.health.lmis.migration.tool.openlmis.ExternalStatus;
import mw.gov.health.lmis.migration.tool.openlmis.fulfillment.domain.Order;
import mw.gov.health.lmis.migration.tool.openlmis.fulfillment.domain.OrderNumberConfiguration;
import mw.gov.health.lmis.migration.tool.openlmis.fulfillment.domain.OrderStatus;
import mw.gov.health.lmis.migration.tool.openlmis.fulfillment.domain.ProofOfDelivery;
import mw.gov.health.lmis.migration.tool.openlmis.fulfillment.repository.OrderRepository;
import mw.gov.health.lmis.migration.tool.openlmis.fulfillment.repository.ProofOfDeliveryRepository;
import mw.gov.health.lmis.migration.tool.openlmis.referencedata.domain.Program;
import mw.gov.health.lmis.migration.tool.openlmis.requisition.domain.Requisition;
import mw.gov.health.lmis.migration.tool.openlmis.requisition.repository.RequisitionRepository;

import java.util.List;

@Component
public class RequisitionWriter implements ItemWriter<List<Requisition>> {
  private static final Logger LOGGER = LoggerFactory.getLogger(RequisitionWriter.class);

  @Autowired
  private RequisitionRepository requisitionRepository;

  @Autowired
  private OrderRepository orderRepository;

  @Autowired
  private ProofOfDeliveryRepository proofOfDeliveryRepository;

  @Autowired
  private ToolProperties toolProperties;

  @Autowired
  private AppContext context;

  /**
   * Writes Requisitions into OpenLMIS database.
   */
  @Override
  public synchronized void write(List<? extends List<Requisition>> items) throws Exception {
    for (int i = 0, length = items.size(); i < length; ++i) {
      List<Requisition> list = items.get(i);

      for (int j = 0, size = list.size(); j < size; ++j) {
        Requisition requisition = list.get(j);

        requisitionRepository.save(requisition);

        if (requisition.getStatus() == ExternalStatus.RELEASED) {
          createOrder(requisition);
        }
      }
    }
  }

  private void createOrder(Requisition requisition) {
    Program program = context.findProgramById(requisition.getProgramId());

    if (null == program) {
      LOGGER.error("Can't find program with id {}", requisition.getProgramId());
    }

    Order orderDb = orderRepository.findByExternalId(requisition.getId());
    Order order = Order.newOrder(requisition, context.getUser());

    if (null != orderDb) {
      order.setId(orderDb.getId());
      order.setOrderCode(orderDb.getOrderCode());
    } else {
      OrderNumberConfiguration config = toolProperties
          .getParameters()
          .getOrderNumberConfiguration();

      order.setOrderCode(config.generateOrderNumber(order, program));
    }

    order.setStatus(OrderStatus.RECEIVED);

    order = orderRepository.save(order);

    ProofOfDelivery podDb = proofOfDeliveryRepository.findByOrder(orderDb);

    if (null != orderDb && null == podDb) {
      LOGGER.error("Can't find POD for requisition: {}", requisition.getId());
      return;
    }

    ProofOfDelivery pod = new ProofOfDelivery(order);

    if (null != podDb) {
      pod.setId(podDb.getId());
    }

    proofOfDeliveryRepository.save(pod);
  }

}
