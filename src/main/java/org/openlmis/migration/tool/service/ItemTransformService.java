package org.openlmis.migration.tool.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.openlmis.migration.tool.domain.Adjustment;
import org.openlmis.migration.tool.domain.AdjustmentType;
import org.openlmis.migration.tool.domain.Comment;
import org.openlmis.migration.tool.domain.Facility;
import org.openlmis.migration.tool.domain.Item;
import org.openlmis.migration.tool.domain.Main;
import org.openlmis.migration.tool.domain.Purpose;
import org.openlmis.migration.tool.openlmis.domain.Requisition;
import org.openlmis.migration.tool.openlmis.domain.RequisitionLineItem;
import org.openlmis.migration.tool.openlmis.domain.RequisitionStatus;
import org.openlmis.migration.tool.openlmis.domain.RequisitionTemplate;
import org.openlmis.migration.tool.openlmis.domain.StockAdjustment;
import org.openlmis.migration.tool.openlmis.dto.FacilityDto;
import org.openlmis.migration.tool.openlmis.dto.OrderableDto;
import org.openlmis.migration.tool.openlmis.dto.ProcessingPeriodDto;
import org.openlmis.migration.tool.openlmis.dto.ProgramDto;
import org.openlmis.migration.tool.openlmis.dto.StockAdjustmentReasonDto;
import org.openlmis.migration.tool.openlmis.repository.OpenLmisFacilityRepository;
import org.openlmis.migration.tool.openlmis.repository.OpenLmisProcessingPeriodRepository;
import org.openlmis.migration.tool.openlmis.repository.OpenLmisProgramRepository;
import org.openlmis.migration.tool.openlmis.repository.OpenLmisRequisitionTemplateRepository;
import org.openlmis.migration.tool.repository.FacilityRepository;
import org.openlmis.migration.tool.repository.ItemRepository;
import org.openlmis.migration.tool.repository.MainRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.chrono.ChronoLocalDateTime;
import java.time.chrono.ChronoZonedDateTime;
import java.time.format.TextStyle;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ItemTransformService {
  private static final Map<String, OrderableDto> ORDERABLES = Maps.newConcurrentMap();
  private static final Map<String, StockAdjustmentReasonDto> REASONS = Maps.newConcurrentMap();

  @Autowired
  private ItemRepository itemRepository;

  @Autowired
  private FacilityRepository facilityRepository;

  @Autowired
  private MainRepository mainRepository;

  @Autowired
  private OpenLmisFacilityRepository openLmisFacilityRepository;

  @Autowired
  private OpenLmisProgramRepository openLmisProgramRepository;

  @Autowired
  private OpenLmisProcessingPeriodRepository openLmisProcessingPeriodRepository;

  @Autowired
  private OpenLmisRequisitionTemplateRepository openLmisRequisitionTemplateRepository;

  public void transform() {
    String format = "%-8s|" +
        "%-57s|" +
        "%-14s|" +
        "%-18s|" +
        "%-16s|" +
        "%-18s|" +
        "%-17s|" +
        "%-17s|" +
        "%-21s|" +
        "%-18s|" +
        "%-20s|" +
        "%-17s|" +
        "%-9s|" +
        "%-13s|" +
        "%-5s%n";

    List<LocalDateTime> processingDates = itemRepository.getProcessingDates();
    processingDates.sort(Comparator.reverseOrder());

    LocalDateTime processingDate = processingDates.get(1);
    Facility facility = facilityRepository.findOne("A");

    Main main = mainRepository.findOne(new Main.ComplexId(facility, processingDate));

    List<Item> items = itemRepository.findByProcessingDateAndFacility(processingDate, facility);
    items.sort((o1, o2) -> {
      int compare = o1.getCategoryProduct().getProduct().getProgramName()
          .compareTo(o2.getCategoryProduct().getProduct().getProgramName());

      if (0 != compare) {
        return compare;
      }

      return o1.getCategoryProduct().getOrder().compareTo(o2.getCategoryProduct().getOrder());
    });

    FacilityDto facilityDto = openLmisFacilityRepository.find(facility);
    ProgramDto programDto = openLmisProgramRepository.find();
    ProcessingPeriodDto processingPeriodDto = openLmisProcessingPeriodRepository
        .find(processingDate);

    RequisitionTemplate template = openLmisRequisitionTemplateRepository.find(programDto);

    Requisition requisition = createRequisition(
        main, items, facilityDto, programDto, processingPeriodDto, template
    );

    System.err.printf("Facility (code): %s (%s)%n", facilityDto.getName(), facilityDto.getCode());
    System.err.printf("Period: %s%n", printPeriod(processingPeriodDto));
    System.err.printf("Date Received: %s Date Shipment Received: %s%n%n", printDate(main.getReceivedDate()), printDate(main.getShipmentReceivedData()));
    System.err.printf(
        format,
        "Product", "Product Description", "Stock on Hand",
        "Adjustment Amount", "Adjustment Type", "Quantity Consumed", "Purpose of Use",
        "Stocked Out Days", "Adjusted Consumption", "Months of Stock", "Calculated Quantity",
        "Reorder Quantity", "Receipts", "Stocked Out?", "Notes"
    );

    items.forEach(item -> {
      RequisitionLineItem requisitionLineItem = requisition
          .getRequisitionLineItems()
          .stream()
          .filter(line -> line.getRemarks().equals(item.getId().toString()))
          .findFirst()
          .orElse(null);
      OrderableDto orderableDto = ORDERABLES.get(item.getProductName());

      System.err.printf(
          format,
          orderableDto.getProductCode(),
          orderableDto.getName(),
          requisitionLineItem.getStockOnHand(),
          requisitionLineItem.getTotalLossesAndAdjustments(),
          item.getAdjustmentType(),
          requisitionLineItem.getTotalConsumedQuantity(),
          countPurposes(item.getPurposes()),
          requisitionLineItem.getTotalStockoutDays(),
          requisitionLineItem.getAdjustedConsumption(),
          getMonthsOfStock(requisitionLineItem),
          requisitionLineItem.getCalculatedOrderQuantity(),
          requisitionLineItem.getRequestedQuantity(),
          requisitionLineItem.getTotalReceivedQuantity(),
          item.getProductStockedOut(),
          printNotes(item.getNotes())
      );
    });

    System.err.println();
    System.err.printf("First input (date):  %-10s (%-10s)%n", main.getCreatedBy(), printDate(requisition.getCreatedDate()));
    System.err.printf("Last changed (date): %-10s (%-10s)%n", main.getModifiedBy(), printDate(requisition.getModifiedDate()));
    System.err.printf("Comment: %s%n", Optional.ofNullable(requisition.getDraftStatusMessage()).orElse(""));
  }

  private Requisition createRequisition(Main main, List<Item> items,
                                        FacilityDto facilityDto,
                                        ProgramDto programDto,
                                        ProcessingPeriodDto processingPeriodDto,
                                        RequisitionTemplate template) {
    Requisition requisition = new Requisition();
    requisition.setFacilityId(facilityDto.getId());
    requisition.setProgramId(programDto.getId());
    requisition.setProcessingPeriodId(processingPeriodDto.getId());
    requisition.setCreatedDate(main.getCreatedDate().atZone(ZoneId.systemDefault()));
    requisition.setModifiedDate(main.getModifiedDate().atZone(ZoneId.systemDefault()));
    requisition.setDraftStatusMessage(main.getNotes());
    requisition.setTemplate(template);
    requisition.setNumberOfMonthsInPeriod(processingPeriodDto.getDurationInMonths());
    requisition.setStatus(RequisitionStatus.INITIATED);

    List<RequisitionLineItem> requisitionLineItems = Lists.newArrayList();

    for (Item item : items) {
      OrderableDto orderableDto = getOrderable(item);

      RequisitionLineItem requisitionLineItem = new RequisitionLineItem();
      requisitionLineItem.setMaxPeriodsOfStock(BigDecimal.valueOf(processingPeriodDto.getDurationInMonths()));
      requisitionLineItem.setRequisition(requisition);
      requisitionLineItem.setSkipped(false);
      requisitionLineItem.setOrderableId(orderableDto.getId());
      requisitionLineItem.setTotalReceivedQuantity(item.getReceipts());
      requisitionLineItem.setTotalConsumedQuantity(item.getDispensedQuantity());

      List<Adjustment> adjustments = item.getAdjustments();
      List<StockAdjustment> stockAdjustments = Lists.newArrayList();
      for (int i = 0, adjustmentsSize = adjustments.size(); i < adjustmentsSize; i++) {
        Adjustment adjustment = adjustments.get(i);
        AdjustmentType adjustmentType = adjustment.getType();
        StockAdjustmentReasonDto stockAdjustmentReasonDto = getStockAdjustmentReason(programDto, i, adjustmentType);

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
          template, REASONS.values(), requisition.getNumberOfMonthsInPeriod()
      );

      requisitionLineItems.add(requisitionLineItem);
    }

    requisition.setRequisitionLineItems(requisitionLineItems);

    Collection<OrderableDto> products = ORDERABLES.values();
    
    requisition.submit(products, null);
    requisition.authorize(products, null);
    requisition.approve(null, products);

    //TODO: convert to order

    return requisition;
  }

  private OrderableDto getOrderable(Item item) {
    OrderableDto orderableDto = ORDERABLES.get(item.getProductName());

    if (null == orderableDto) {
      orderableDto = new OrderableDto();
      orderableDto.setId(UUID.randomUUID());
      orderableDto.setProductCode(item.getProduct().getProductId());
      orderableDto.setName(item.getProductName());

      ORDERABLES.put(item.getProductName(), orderableDto);
    }
    
    return orderableDto;
  }

  private StockAdjustmentReasonDto getStockAdjustmentReason(ProgramDto programDto, int i,
                                                            AdjustmentType adjustmentType) {
    StockAdjustmentReasonDto stockAdjustmentReasonDto = REASONS.get(adjustmentType.getCode());

    if (null == stockAdjustmentReasonDto) {
      stockAdjustmentReasonDto = new StockAdjustmentReasonDto();
      stockAdjustmentReasonDto.setId(UUID.randomUUID());
      stockAdjustmentReasonDto.setProgramId(programDto.getId());
      stockAdjustmentReasonDto.setName(adjustmentType.getCode());
      stockAdjustmentReasonDto.setDescription(adjustmentType.getName());
      stockAdjustmentReasonDto.setAdditive(!adjustmentType.getNegative());
      stockAdjustmentReasonDto.setDisplayOrder(i);

      REASONS.put(adjustmentType.getCode(), stockAdjustmentReasonDto);
    }
    
    return stockAdjustmentReasonDto;
  }

  private int countPurposes(List<Purpose> purposes) {
    return null != purposes
        ? purposes.stream().map(Purpose::getQuantity).reduce(0, (a, b) -> a + b)
        : 0;
  }

  private String printNotes(List<Comment> comments) {
    if (null == comments || comments.isEmpty()) {
      return "";
    }

    return comments
        .stream()
        .map(note -> note.getType().getName() + ": " + note.getComment())
        .collect(Collectors.joining(", "));
  }

  private String printDate(ChronoLocalDateTime dateTime) {
    return null == dateTime ? "" : dateTime.toLocalDate().toString();
  }

  private String printDate(ChronoZonedDateTime dateTime) {
    return null == dateTime ? "" : dateTime.toLocalDate().toString();
  }

  private String printPeriod(ProcessingPeriodDto period) {
    return String.format(
        "%s-%s %d",
        period.getStartDate().getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
        period.getEndDate().getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
        period.getStartDate().getYear()
    );
  }

  private Double getMonthsOfStock(RequisitionLineItem requisitionLineItem) {
    if (0 == requisitionLineItem.getAdjustedConsumption()) {
      return 0.0;
    }

    return BigDecimal.valueOf(requisitionLineItem.getStockOnHand())
        .divide(
            BigDecimal.valueOf(requisitionLineItem.getAdjustedConsumption()),
            1,
            BigDecimal.ROUND_HALF_UP
        )
        .doubleValue();
  }

}
