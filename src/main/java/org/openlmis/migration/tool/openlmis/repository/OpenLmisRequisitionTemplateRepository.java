package org.openlmis.migration.tool.openlmis.repository;

import static org.openlmis.migration.tool.openlmis.domain.RequisitionLineItem.ADJUSTED_CONSUMPTION;
import static org.openlmis.migration.tool.openlmis.domain.RequisitionLineItem.APPROVED_QUANTITY;
import static org.openlmis.migration.tool.openlmis.domain.RequisitionLineItem.AVERAGE_CONSUMPTION;
import static org.openlmis.migration.tool.openlmis.domain.RequisitionLineItem.BEGINNING_BALANCE;
import static org.openlmis.migration.tool.openlmis.domain.RequisitionLineItem.CALCULATED_ORDER_QUANTITY;
import static org.openlmis.migration.tool.openlmis.domain.RequisitionLineItem.DISPENSING_UNIT;
import static org.openlmis.migration.tool.openlmis.domain.RequisitionLineItem.MAXIMUM_STOCK_QUANTITY;
import static org.openlmis.migration.tool.openlmis.domain.RequisitionLineItem.NUMBER_OF_NEW_PATIENTS_ADDED;
import static org.openlmis.migration.tool.openlmis.domain.RequisitionLineItem.PACKS_TO_SHIP;
import static org.openlmis.migration.tool.openlmis.domain.RequisitionLineItem.PRICE_PER_PACK;
import static org.openlmis.migration.tool.openlmis.domain.RequisitionLineItem.PRODUCT;
import static org.openlmis.migration.tool.openlmis.domain.RequisitionLineItem.PRODUCT_CODE;
import static org.openlmis.migration.tool.openlmis.domain.RequisitionLineItem.REMARKS_COLUMN;
import static org.openlmis.migration.tool.openlmis.domain.RequisitionLineItem.REQUESTED_QUANTITY;
import static org.openlmis.migration.tool.openlmis.domain.RequisitionLineItem.REQUESTED_QUANTITY_EXPLANATION;
import static org.openlmis.migration.tool.openlmis.domain.RequisitionLineItem.SKIPPED_COLUMN;
import static org.openlmis.migration.tool.openlmis.domain.RequisitionLineItem.STOCK_ON_HAND;
import static org.openlmis.migration.tool.openlmis.domain.RequisitionLineItem.TOTAL_COLUMN;
import static org.openlmis.migration.tool.openlmis.domain.RequisitionLineItem.TOTAL_CONSUMED_QUANTITY;
import static org.openlmis.migration.tool.openlmis.domain.RequisitionLineItem.TOTAL_COST;
import static org.openlmis.migration.tool.openlmis.domain.RequisitionLineItem.TOTAL_LOSSES_AND_ADJUSTMENTS;
import static org.openlmis.migration.tool.openlmis.domain.RequisitionLineItem.TOTAL_RECEIVED_QUANTITY;
import static org.openlmis.migration.tool.openlmis.domain.RequisitionLineItem.TOTAL_STOCKOUT_DAYS;
import static org.openlmis.migration.tool.openlmis.domain.SourceType.CALCULATED;
import static org.openlmis.migration.tool.openlmis.domain.SourceType.REFERENCE_DATA;
import static org.openlmis.migration.tool.openlmis.domain.SourceType.USER_INPUT;

import com.google.common.collect.ImmutableMap;

import org.openlmis.migration.tool.openlmis.domain.AvailableRequisitionColumn;
import org.openlmis.migration.tool.openlmis.domain.RequisitionTemplate;
import org.openlmis.migration.tool.openlmis.domain.RequisitionTemplateColumn;
import org.openlmis.migration.tool.openlmis.domain.SourceType;
import org.openlmis.migration.tool.openlmis.dto.ProgramDto;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class OpenLmisRequisitionTemplateRepository {

  public RequisitionTemplate find(ProgramDto program) {
    RequisitionTemplate template = new RequisitionTemplate();
    template.setProgramId(program.getId());
    template.setNumberOfPeriodsToAverage(2);
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
        .put(TOTAL_RECEIVED_QUANTITY, create(TOTAL_RECEIVED_QUANTITY, "B", EnumSet.of(USER_INPUT), true ))
        .put(TOTAL_CONSUMED_QUANTITY, create(TOTAL_CONSUMED_QUANTITY, "C", EnumSet.of(USER_INPUT, CALCULATED), true))
        .put(TOTAL_LOSSES_AND_ADJUSTMENTS, create(TOTAL_LOSSES_AND_ADJUSTMENTS, "D", EnumSet.of(USER_INPUT), true))
        .put(TOTAL_STOCKOUT_DAYS, create(TOTAL_STOCKOUT_DAYS, "X", EnumSet.of(USER_INPUT), true))
        .put(STOCK_ON_HAND, create(STOCK_ON_HAND, "E", EnumSet.of(USER_INPUT, CALCULATED), true))
        .put(AVERAGE_CONSUMPTION, create(AVERAGE_CONSUMPTION, "P", EnumSet.of(CALCULATED), false))
        .put(CALCULATED_ORDER_QUANTITY, create(CALCULATED_ORDER_QUANTITY, "I", EnumSet.of(CALCULATED), true))
        .put(PRICE_PER_PACK, create(PRICE_PER_PACK, "T", EnumSet.of(REFERENCE_DATA), false))
        .put(DISPENSING_UNIT, create(DISPENSING_UNIT, "U", EnumSet.of(REFERENCE_DATA), false))
        .put(REQUESTED_QUANTITY, create(REQUESTED_QUANTITY, "J", EnumSet.of(USER_INPUT), true))
        .put(REQUESTED_QUANTITY_EXPLANATION, create(REQUESTED_QUANTITY_EXPLANATION, "W", EnumSet.of(USER_INPUT), true))
        .put(APPROVED_QUANTITY, create(APPROVED_QUANTITY, "K", EnumSet.of(USER_INPUT), true))
        .put(REMARKS_COLUMN, create(REMARKS_COLUMN, "L", EnumSet.of(USER_INPUT), true))
        .put(TOTAL_COLUMN, create(TOTAL_COLUMN, "Y", EnumSet.of(CALCULATED), true))
        .put(PACKS_TO_SHIP, create(PACKS_TO_SHIP, "V", EnumSet.of(CALCULATED), false))
        .put(NUMBER_OF_NEW_PATIENTS_ADDED, create(NUMBER_OF_NEW_PATIENTS_ADDED, "F", EnumSet.of(USER_INPUT), false))
        .put(TOTAL_COST, create(TOTAL_COST, "Q", EnumSet.of(CALCULATED), false))
        .put(ADJUSTED_CONSUMPTION, create(ADJUSTED_CONSUMPTION, "N", EnumSet.of(CALCULATED), true))
        .put(MAXIMUM_STOCK_QUANTITY, create(MAXIMUM_STOCK_QUANTITY, "H", EnumSet.of(CALCULATED), false))
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
