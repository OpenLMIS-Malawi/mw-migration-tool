package mw.gov.health.lmis.migration.tool.batch;

import com.google.common.collect.Maps;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import mw.gov.health.lmis.migration.tool.AppContext;
import mw.gov.health.lmis.migration.tool.openlmis.referencedata.domain.ProcessingPeriod;
import mw.gov.health.lmis.migration.tool.openlmis.requisition.domain.Requisition;
import mw.gov.health.lmis.migration.tool.openlmis.requisition.domain.RequisitionLineItem;
import mw.gov.health.lmis.migration.tool.openlmis.requisition.repository.RequisitionRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
class ProductHelper {
  private static final Map<Signature, Integer> CLOSING_BALANCES = Maps.newConcurrentMap();

  @Autowired
  private RequisitionRepository requisitionRepository;

  @Autowired
  private AppContext context;

  void add(Requisition requisition) {
    for (int i = 0, size = requisition.getRequisitionLineItems().size(); i < size; ++i) {
      RequisitionLineItem line = requisition.getRequisitionLineItems().get(i);
      Signature signature = new Signature(requisition, line);

      CLOSING_BALANCES.put(signature, line.getStockOnHand());
    }
  }

  Integer getClosingBalance(Requisition requisition, UUID product) {
    ProcessingPeriod previousPeriod = context
        .findPreviousPeriod(requisition.getProcessingPeriodId());

    if (null == previousPeriod) {
      return 0;
    }

    Signature signature = new Signature(requisition, previousPeriod.getId(), product);

    return getClosingBalance(signature, false);
  }

  private Integer getClosingBalance(Signature signature, boolean checked) {
    if (checked || CLOSING_BALANCES.containsKey(signature)) {
      Integer value = CLOSING_BALANCES.computeIfAbsent(signature, key -> 0);
      return Optional.ofNullable(value).orElse(0);
    }

    Requisition previousRequisition = getPreviousRequisition(signature);

    if (null != previousRequisition) {
      add(previousRequisition);
    }

    return getClosingBalance(signature, true);
  }

  private Requisition getPreviousRequisition(Signature signature) {
    List<Requisition> requisitionsByPeriod = requisitionRepository
        .findByFacilityIdAndProgramIdAndProcessingPeriodId(
            signature.getFacility(), signature.getProgram(), signature.getPeriod()
        );

    return requisitionsByPeriod.isEmpty() ? null : requisitionsByPeriod.get(0);
  }

  @EqualsAndHashCode
  @Getter
  private static final class Signature {
    private final UUID facility;
    private final UUID program;
    private final UUID period;
    private final UUID product;

    Signature(Requisition requisition, RequisitionLineItem line) {
      this(requisition, requisition.getProcessingPeriodId(), line.getOrderableId());
    }

    Signature(Requisition requisition, UUID period, UUID product) {
      this.facility = requisition.getFacilityId();
      this.program = requisition.getProgramId();
      this.period = period;
      this.product = product;
    }

  }
}
