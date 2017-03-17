package org.openlmis.migration.tool.batch;

import com.google.common.collect.Lists;

import org.openlmis.migration.tool.domain.Adjustment;
import org.openlmis.migration.tool.domain.AdjustmentType;
import org.openlmis.migration.tool.domain.Item;
import org.openlmis.migration.tool.domain.Main;
import org.openlmis.migration.tool.openlmis.referencedata.domain.Facility;
import org.openlmis.migration.tool.openlmis.referencedata.domain.Orderable;
import org.openlmis.migration.tool.openlmis.referencedata.domain.ProcessingPeriod;
import org.openlmis.migration.tool.openlmis.referencedata.domain.Program;
import org.openlmis.migration.tool.openlmis.referencedata.domain.StockAdjustmentReason;
import org.openlmis.migration.tool.openlmis.referencedata.repository.OpenLmisFacilityRepository;
import org.openlmis.migration.tool.openlmis.referencedata.repository.OpenLmisOrderableRepository;
import org.openlmis.migration.tool.openlmis.referencedata.repository.OpenLmisProcessingPeriodRepository;
import org.openlmis.migration.tool.openlmis.referencedata.repository.OpenLmisProgramRepository;
import org.openlmis.migration.tool.openlmis.referencedata.repository.OpenLmisStockAdjustmentReasonRepository;
import org.openlmis.migration.tool.openlmis.requisition.domain.Requisition;
import org.openlmis.migration.tool.openlmis.requisition.domain.RequisitionLineItem;
import org.openlmis.migration.tool.openlmis.requisition.domain.RequisitionStatus;
import org.openlmis.migration.tool.openlmis.requisition.domain.RequisitionTemplate;
import org.openlmis.migration.tool.openlmis.requisition.domain.StockAdjustment;
import org.openlmis.migration.tool.openlmis.requisition.repository.OpenLmisRequisitionTemplateRepository;
import org.openlmis.migration.tool.repository.ItemRepository;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Component
public class MainProcessor implements ItemProcessor<Main, Requisition> {

  @Autowired
  private ItemRepository itemRepository;

  @Autowired
  private OpenLmisFacilityRepository openLmisFacilityRepository;

  @Autowired
  private OpenLmisProgramRepository openLmisProgramRepository;

  @Autowired
  private OpenLmisProcessingPeriodRepository openLmisProcessingPeriodRepository;

  @Autowired
  private OpenLmisRequisitionTemplateRepository openLmisRequisitionTemplateRepository;

  @Autowired
  private OpenLmisStockAdjustmentReasonRepository openLmisStockAdjustmentReasonRepository;

  @Autowired
  private OpenLmisOrderableRepository openLmisOrderableRepository;

  /**
   * Converts the given {@link Main} object into {@link Requisition} object.
   */
  @Override
  public Requisition process(Main main) {
    List<Item> items = itemRepository.findByProcessingDateAndFacility(
        main.getId().getProcessingDate(), main.getId().getFacility()
    );

    items.sort((o1, o2) -> {
      int compare = o1.getCategoryProduct().getProduct().getProgramName()
          .compareTo(o2.getCategoryProduct().getProduct().getProgramName());

      if (0 != compare) {
        return compare;
      }

      return o1.getCategoryProduct().getOrder().compareTo(o2.getCategoryProduct().getOrder());
    });

    return createRequisition(main, items);
  }

  private Requisition createRequisition(Main main, List<Item> items) {
    ProcessingPeriod period = openLmisProcessingPeriodRepository
        .findByStartDate(main.getId().getProcessingDate());

    Program program = openLmisProgramRepository.findByName(main);
    RequisitionTemplate template = openLmisRequisitionTemplateRepository
        .findByProgramId(program.getId());

    Requisition requisition = initRequisition(main, template, program, period);

    List<RequisitionLineItem> requisitionLineItems = Lists.newArrayList();

    for (Item item : items) {
      RequisitionLineItem requisitionLineItem = createLine(
          item, requisition, template, program, period
      );

      requisitionLineItems.add(requisitionLineItem);
    }

    requisition.setRequisitionLineItems(requisitionLineItems);

    List<Orderable> products = Lists.newArrayList(openLmisOrderableRepository.findAll());

    requisition.submit(products, null);
    requisition.authorize(products, null);
    requisition.approve(null, products);

    //TODO: convert to order

    return requisition;
  }

  private Requisition initRequisition(Main main, RequisitionTemplate template, Program programDto,
                                      ProcessingPeriod processingPeriodDto) {
    org.openlmis.migration.tool.domain.Facility mainFacility = main.getId().getFacility();
    Facility facility = openLmisFacilityRepository
        .findByNameAndCode(mainFacility.getName(), mainFacility.getCode());

    Requisition requisition = new Requisition();
    requisition.setFacilityId(facility.getId());
    requisition.setProgramId(programDto.getId());
    requisition.setProcessingPeriodId(processingPeriodDto.getId());
    requisition.setCreatedDate(safeNull(main.getCreatedDate()));
    requisition.setModifiedDate(safeNull(main.getModifiedDate()));
    requisition.setDraftStatusMessage(main.getNotes());
    requisition.setTemplate(template);
    requisition.setNumberOfMonthsInPeriod(processingPeriodDto.getDurationInMonths());
    requisition.setStatus(RequisitionStatus.INITIATED);

    return requisition;
  }

  private ZonedDateTime safeNull(LocalDateTime dateTime) {
    return null == dateTime
        ? null
        : dateTime.atZone(ZoneId.of("UTC"));
  }

  private RequisitionLineItem createLine(Item item, Requisition requisition,
                                         RequisitionTemplate template, Program programDto,
                                         ProcessingPeriod processingPeriodDto) {
    Orderable orderableDto = openLmisOrderableRepository.findByName(item);

    RequisitionLineItem requisitionLineItem = new RequisitionLineItem();
    requisitionLineItem.setMaxPeriodsOfStock(
        BigDecimal.valueOf(processingPeriodDto.getDurationInMonths())
    );
    requisitionLineItem.setRequisition(requisition);
    requisitionLineItem.setSkipped(false);
    requisitionLineItem.setOrderableId(orderableDto.getId());
    requisitionLineItem.setTotalReceivedQuantity(item.getReceipts());
    requisitionLineItem.setTotalConsumedQuantity(item.getDispensedQuantity());

    List<StockAdjustment> stockAdjustments = Lists.newArrayList();
    for (Adjustment adjustment : item.getAdjustments()) {
      AdjustmentType adjustmentType = adjustment.getType();
      StockAdjustmentReason stockAdjustmentReasonDto = openLmisStockAdjustmentReasonRepository
          .findByProgram(programDto, adjustmentType);

      StockAdjustment stockAdjustment = new StockAdjustment();
      stockAdjustment.setReasonId(stockAdjustmentReasonDto.getId());
      stockAdjustment.setQuantity(adjustment.getQuantity());

      stockAdjustments.add(stockAdjustment);
    }

    requisitionLineItem.setStockAdjustments(stockAdjustments);
    requisitionLineItem.setTotalStockoutDays(item.getStockedOutDays().intValue());
    requisitionLineItem.setStockOnHand(item.getClosingBalance());
    requisitionLineItem.setCalculatedOrderQuantity(item.getCalculatedRequiredQuantity());
    requisitionLineItem.setRequestedQuantity(item.getRequiredQuantity());
    requisitionLineItem.setRequestedQuantityExplanation("lagacy data");
    requisitionLineItem.setAdjustedConsumption(item.getAdjustedDispensedQuantity());

    requisitionLineItem.setRemarks(item.getId().toString());

    requisitionLineItem.calculateAndSetFields(
        template, Lists.newArrayList(openLmisStockAdjustmentReasonRepository.findAll()),
        requisition.getNumberOfMonthsInPeriod()
    );
    return requisitionLineItem;
  }

}