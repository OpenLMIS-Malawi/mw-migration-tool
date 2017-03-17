package org.openlmis.migration.tool.openlmis.requisition.repository;

import static org.openlmis.migration.tool.openlmis.requisition.domain.RequisitionLineItem.ADJUSTED_CONSUMPTION;
import static org.openlmis.migration.tool.openlmis.requisition.domain.RequisitionLineItem.APPROVED_QUANTITY;
import static org.openlmis.migration.tool.openlmis.requisition.domain.RequisitionLineItem.AVERAGE_CONSUMPTION;
import static org.openlmis.migration.tool.openlmis.requisition.domain.RequisitionLineItem.BEGINNING_BALANCE;
import static org.openlmis.migration.tool.openlmis.requisition.domain.RequisitionLineItem.CALCULATED_ORDER_QUANTITY;
import static org.openlmis.migration.tool.openlmis.requisition.domain.RequisitionLineItem.DISPENSING_UNIT;
import static org.openlmis.migration.tool.openlmis.requisition.domain.RequisitionLineItem.MAXIMUM_STOCK_QUANTITY;
import static org.openlmis.migration.tool.openlmis.requisition.domain.RequisitionLineItem.NUMBER_OF_NEW_PATIENTS_ADDED;
import static org.openlmis.migration.tool.openlmis.requisition.domain.RequisitionLineItem.PACKS_TO_SHIP;
import static org.openlmis.migration.tool.openlmis.requisition.domain.RequisitionLineItem.PRICE_PER_PACK;
import static org.openlmis.migration.tool.openlmis.requisition.domain.RequisitionLineItem.PRODUCT;
import static org.openlmis.migration.tool.openlmis.requisition.domain.RequisitionLineItem.PRODUCT_CODE;
import static org.openlmis.migration.tool.openlmis.requisition.domain.RequisitionLineItem.REMARKS_COLUMN;
import static org.openlmis.migration.tool.openlmis.requisition.domain.RequisitionLineItem.REQUESTED_QUANTITY;
import static org.openlmis.migration.tool.openlmis.requisition.domain.RequisitionLineItem.REQUESTED_QUANTITY_EXPLANATION;
import static org.openlmis.migration.tool.openlmis.requisition.domain.RequisitionLineItem.SKIPPED_COLUMN;
import static org.openlmis.migration.tool.openlmis.requisition.domain.RequisitionLineItem.STOCK_ON_HAND;
import static org.openlmis.migration.tool.openlmis.requisition.domain.RequisitionLineItem.TOTAL_COLUMN;
import static org.openlmis.migration.tool.openlmis.requisition.domain.RequisitionLineItem.TOTAL_CONSUMED_QUANTITY;
import static org.openlmis.migration.tool.openlmis.requisition.domain.RequisitionLineItem.TOTAL_COST;
import static org.openlmis.migration.tool.openlmis.requisition.domain.RequisitionLineItem.TOTAL_LOSSES_AND_ADJUSTMENTS;
import static org.openlmis.migration.tool.openlmis.requisition.domain.RequisitionLineItem.TOTAL_RECEIVED_QUANTITY;
import static org.openlmis.migration.tool.openlmis.requisition.domain.RequisitionLineItem.TOTAL_STOCKOUT_DAYS;
import static org.openlmis.migration.tool.openlmis.requisition.domain.SourceType.CALCULATED;
import static org.openlmis.migration.tool.openlmis.requisition.domain.SourceType.REFERENCE_DATA;
import static org.openlmis.migration.tool.openlmis.requisition.domain.SourceType.USER_INPUT;

import com.google.common.collect.ImmutableMap;

import org.openlmis.migration.tool.domain.SystemDefault;
import org.openlmis.migration.tool.openlmis.InMemoryRepository;
import org.openlmis.migration.tool.openlmis.requisition.domain.AvailableRequisitionColumn;
import org.openlmis.migration.tool.openlmis.requisition.domain.RequisitionTemplate;
import org.openlmis.migration.tool.openlmis.requisition.domain.RequisitionTemplateColumn;
import org.openlmis.migration.tool.openlmis.requisition.domain.SourceType;
import org.openlmis.migration.tool.repository.SystemDefaultRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Repository
public class OpenLmisRequisitionTemplateRepository extends InMemoryRepository<RequisitionTemplate> {

  @Autowired
  private SystemDefaultRepository systemDefaultRepository;

  /**
   * Finds Requisiton template for the given program id.
   */
  public RequisitionTemplate findByProgramId(UUID programId) {
    RequisitionTemplate found = database
        .values()
        .stream()
        .filter(template -> programId.equals(template.getProgramId()))
        .findFirst()
        .orElse(null);

    if (null == found) {
      save(createTemplate(programId));
      return findByProgramId(programId);
    }

    return found;
  }

  private RequisitionTemplate createTemplate(UUID programId) {
    SystemDefault systemDefault = systemDefaultRepository
        .findAll()
        .iterator()
        .next();

    RequisitionTemplate template = new RequisitionTemplate();
    template.setProgramId(programId);
    template.setNumberOfPeriodsToAverage(systemDefault.getNumberOfPeriodsToAverage().intValue());
    template.setColumnsMap(getRequisitionTemplateColumnMap());

    template.changeColumnSource(TOTAL_CONSUMED_QUANTITY, USER_INPUT);
    template.changeColumnSource(STOCK_ON_HAND, USER_INPUT);

    return template;
  }

  private Map<String, RequisitionTemplateColumn> getRequisitionTemplateColumnMap() {
    return ImmutableMap
        .<String, RequisitionTemplateColumn>builder()
        .put(SKIPPED_COLUMN, create(SKIPPED_COLUMN, "S", EnumSet.of(USER_INPUT), true))
        .put(PRODUCT_CODE, create(PRODUCT_CODE, "O", EnumSet.of(REFERENCE_DATA), true))
        .put(PRODUCT, create(PRODUCT, "R", EnumSet.of(REFERENCE_DATA), true))
        .put(BEGINNING_BALANCE, create(BEGINNING_BALANCE, "A", EnumSet.of(USER_INPUT), false))
        .put(
            TOTAL_RECEIVED_QUANTITY,
            create(TOTAL_RECEIVED_QUANTITY, "B", EnumSet.of(USER_INPUT), true)
        )
        .put(
            TOTAL_CONSUMED_QUANTITY,
            create(TOTAL_CONSUMED_QUANTITY, "C", EnumSet.of(USER_INPUT, CALCULATED), true)
        )
        .put(
            TOTAL_LOSSES_AND_ADJUSTMENTS,
            create(TOTAL_LOSSES_AND_ADJUSTMENTS, "D", EnumSet.of(USER_INPUT), true)
        )
        .put(TOTAL_STOCKOUT_DAYS, create(TOTAL_STOCKOUT_DAYS, "X", EnumSet.of(USER_INPUT), true))
        .put(STOCK_ON_HAND, create(STOCK_ON_HAND, "E", EnumSet.of(USER_INPUT, CALCULATED), true))
        .put(AVERAGE_CONSUMPTION, create(AVERAGE_CONSUMPTION, "P", EnumSet.of(CALCULATED), false))
        .put(
            CALCULATED_ORDER_QUANTITY,
            create(CALCULATED_ORDER_QUANTITY, "I", EnumSet.of(CALCULATED), true)
        )
        .put(PRICE_PER_PACK, create(PRICE_PER_PACK, "T", EnumSet.of(REFERENCE_DATA), false))
        .put(DISPENSING_UNIT, create(DISPENSING_UNIT, "U", EnumSet.of(REFERENCE_DATA), false))
        .put(REQUESTED_QUANTITY, create(REQUESTED_QUANTITY, "J", EnumSet.of(USER_INPUT), true))
        .put(
            REQUESTED_QUANTITY_EXPLANATION,
            create(REQUESTED_QUANTITY_EXPLANATION, "W", EnumSet.of(USER_INPUT), true)
        )
        .put(APPROVED_QUANTITY, create(APPROVED_QUANTITY, "K", EnumSet.of(USER_INPUT), true))
        .put(REMARKS_COLUMN, create(REMARKS_COLUMN, "L", EnumSet.of(USER_INPUT), true))
        .put(TOTAL_COLUMN, create(TOTAL_COLUMN, "Y", EnumSet.of(CALCULATED), true))
        .put(PACKS_TO_SHIP, create(PACKS_TO_SHIP, "V", EnumSet.of(CALCULATED), false))
        .put(
            NUMBER_OF_NEW_PATIENTS_ADDED,
            create(NUMBER_OF_NEW_PATIENTS_ADDED, "F", EnumSet.of(USER_INPUT), false)
        )
        .put(TOTAL_COST, create(TOTAL_COST, "Q", EnumSet.of(CALCULATED), false))
        .put(ADJUSTED_CONSUMPTION, create(ADJUSTED_CONSUMPTION, "N", EnumSet.of(CALCULATED), true))
        .put(
            MAXIMUM_STOCK_QUANTITY,
            create(MAXIMUM_STOCK_QUANTITY, "H", EnumSet.of(CALCULATED), false)
        )
        .build();
  }

  private RequisitionTemplateColumn create(String name, String indicator,
                                           Set<SourceType> sources, boolean displayed) {
    AvailableRequisitionColumn definition = new AvailableRequisitionColumn();
    definition.setId(java.util.UUID.randomUUID());
    definition.setName(name);
    definition.setIndicator(indicator);
    definition.setIsDisplayRequired(false);
    definition.setSources(sources);

    RequisitionTemplateColumn requisitionTemplateColumn = new RequisitionTemplateColumn(definition);
    requisitionTemplateColumn.setName(name);
    requisitionTemplateColumn.setIsDisplayed(displayed);
    requisitionTemplateColumn.setSource(sources.iterator().next());
    requisitionTemplateColumn.setLabel(name.toLowerCase(Locale.ENGLISH));

    return requisitionTemplateColumn;
  }

}
