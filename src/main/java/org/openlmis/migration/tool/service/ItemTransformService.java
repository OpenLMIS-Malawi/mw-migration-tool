package org.openlmis.migration.tool.service;

import static org.apache.commons.lang.BooleanUtils.isTrue;

import com.beust.jcommander.internal.Lists;

import org.openlmis.migration.tool.domain.Adjustment;
import org.openlmis.migration.tool.domain.Comment;
import org.openlmis.migration.tool.domain.Facility;
import org.openlmis.migration.tool.domain.Item;
import org.openlmis.migration.tool.domain.Main;
import org.openlmis.migration.tool.domain.Purpose;
import org.openlmis.migration.tool.openlmis.domain.Requisition;
import org.openlmis.migration.tool.openlmis.domain.RequisitionLineItem;
import org.openlmis.migration.tool.openlmis.domain.RequisitionTemplate;
import org.openlmis.migration.tool.openlmis.dto.FacilityDto;
import org.openlmis.migration.tool.openlmis.dto.ProcessingPeriodDto;
import org.openlmis.migration.tool.openlmis.dto.ProgramDto;
import org.openlmis.migration.tool.openlmis.repository.OpenLmisFacilityRepository;
import org.openlmis.migration.tool.openlmis.repository.OpenLmisProcessingPeriodRepository;
import org.openlmis.migration.tool.openlmis.repository.OpenLmisProgramRepository;
import org.openlmis.migration.tool.openlmis.repository.OpenLmisRequisitionTemplateRepository;
import org.openlmis.migration.tool.repository.FacilityRepository;
import org.openlmis.migration.tool.repository.ItemRepository;
import org.openlmis.migration.tool.repository.MainRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.chrono.ChronoLocalDateTime;
import java.time.chrono.ChronoZonedDateTime;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ItemTransformService {

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
    String format = "%-43s|" +
        "%-8s|" +
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
        "Category Name", "Product", "Product Description", "Stock on Hand",
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

      System.err.printf(
          format,
          item.getCategoryProduct().getProduct().getProgramName(),
          item.getProduct().getProductId(), item.getProductName(),
          requisitionLineItem.getStockOnHand(),
          countAdjustments(item.getAdjustments()), item.getAdjustmentType(),
          requisitionLineItem.getTotalConsumedQuantity(), countPurposes(item.getPurposes()),
          requisitionLineItem.getTotalStockoutDays(),
          requisitionLineItem.getAdjustedConsumption(),
          "<months_of_stock>", requisitionLineItem.getCalculatedOrderQuantity(),
          requisitionLineItem.getRequestedQuantity(),
          item.getReceipts(), item.getProductStockedOut(),
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

    List<RequisitionLineItem> requisitionLineItems = Lists.newArrayList();

    for (Item item : items) {
      RequisitionLineItem requisitionLineItem = new RequisitionLineItem();
      requisitionLineItem.setRequisition(requisition);
      requisitionLineItem.setOrderableId(UUID.randomUUID());
      requisitionLineItem.setStockOnHand(item.getClosingBalance());
      requisitionLineItem.setTotalConsumedQuantity(item.getDispensedQuantity());
      requisitionLineItem.setTotalStockoutDays(item.getStockedOutDays().intValue());
      requisitionLineItem.setAdjustedConsumption(item.getAdjustedDispensedQuantity());
      requisitionLineItem.setRequestedQuantity(item.getRequiredQuantity());
      requisitionLineItem.setRemarks(item.getId().toString());

      // TODO: add adjustments

      // TODO: fix this
      //requisitionLineItem.calculateAndSetFields(
      //    template, Collections.emptyList(), requisition.getNumberOfMonthsInPeriod()
      //);

      requisitionLineItems.add(requisitionLineItem);
    }

    requisition.setRequisitionLineItems(requisitionLineItems);

    return requisition;
  }

  private int countPurposes(List<Purpose> purposes) {
    return null != purposes
        ? purposes.stream().map(Purpose::getQuantity).reduce(0, (a, b) -> a + b)
        : 0;
  }

  private int countAdjustments(List<Adjustment> adjustments) {
    if (null == adjustments) {
      return 0;
    }

    return adjustments
        .stream()
        .map(a -> a.getQuantity() * (isTrue(a.getType().getNegative()) ? -1 : 1))
        .reduce(0, (a, b) -> a + b);
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

}
