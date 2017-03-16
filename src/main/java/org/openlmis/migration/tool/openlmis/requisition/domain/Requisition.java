/*
 * This program is part of the OpenLMIS logistics management information system platform software.
 * Copyright © 2017 VillageReach
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *  
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
 * See the GNU Affero General Public License for more details. You should have received a copy of
 * the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses.  For additional information contact info@OpenLMIS.org. 
 */

package org.openlmis.migration.tool.openlmis.requisition.domain;

import static java.util.Objects.isNull;
import static org.openlmis.migration.tool.openlmis.requisition.domain.OpenLmisNumberUtils.zeroIfNull;
import static org.openlmis.migration.tool.openlmis.requisition.domain.RequisitionLineItem.ADJUSTED_CONSUMPTION;
import static org.openlmis.migration.tool.openlmis.requisition.domain.RequisitionLineItem.AVERAGE_CONSUMPTION;
import static org.openlmis.migration.tool.openlmis.requisition.domain.RequisitionLineItem.CALCULATED_ORDER_QUANTITY;
import static org.openlmis.migration.tool.openlmis.requisition.domain.RequisitionStatus.INITIATED;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Type;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.openlmis.migration.tool.openlmis.BaseEntity;
import org.openlmis.migration.tool.openlmis.CurrencyConfig;
import org.openlmis.migration.tool.openlmis.fulfillment.domain.ProofOfDelivery;
import org.openlmis.migration.tool.openlmis.fulfillment.domain.ProofOfDeliveryLineItem;
import org.openlmis.migration.tool.openlmis.referencedata.domain.FacilityTypeApprovedProduct;
import org.openlmis.migration.tool.openlmis.referencedata.domain.Orderable;
import org.openlmis.migration.tool.openlmis.referencedata.domain.StockAdjustmentReason;
import org.openlmis.migration.tool.openlmis.requisition.RequisitionHelper;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

@SuppressWarnings("PMD.TooManyMethods")
@Entity
@Table(name = "requisitions")
@NoArgsConstructor
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class Requisition extends BaseTimestampedEntity {

  public static final String FACILITY_ID = "facilityId";
  public static final String PROGRAM_ID = "programId";
  public static final String PROCESSING_PERIOD_ID = "processingPeriodId";
  public static final String TOTAL_CONSUMED_QUANTITY = "totalConsumedQuantity";
  public static final String STOCK_ON_HAND = "stockOnHand";
  public static final String SUPERVISORY_NODE_ID = "supervisoryNodeId";
  public static final String EMERGENCY = "emergency";
  public static final String MODIFIED_DATE = "modifiedDate";
  public static final String STATUS = "status";

  @OneToMany(
      mappedBy = "requisition",
      cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.REMOVE},
      fetch = FetchType.EAGER,
      orphanRemoval = true)
  @Fetch(FetchMode.SELECT)
  @Getter
  @Setter
  private List<RequisitionLineItem> requisitionLineItems;

  @Getter
  private String draftStatusMessage;

  @ManyToOne
  @JoinColumn(name = "templateId", nullable = false)
  @Getter
  @Setter
  private RequisitionTemplate template;

  @Column(nullable = false)
  @Getter
  @Setter
  @Type(type = UUID_TYPE)
  private UUID facilityId;

  @Column(nullable = false)
  @Getter
  @Setter
  @Type(type = UUID_TYPE)
  private UUID programId;

  @Column(nullable = false)
  @Getter
  @Setter
  @Type(type = UUID_TYPE)
  private UUID processingPeriodId;

  @Getter
  @Setter
  @Type(type = UUID_TYPE)
  private UUID supplyingFacilityId;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  @Getter
  @Setter
  private RequisitionStatus status;

  /*
   * Used to convey a small subset of the requisition-related data collected via JaVers' audit-log.
   * This is primarily used to allow users the ability to see when the RequisitionStatus changed.
   * A more holistic approach to audit logging is forthcoming.
   */
  @Transient
  @Getter
  @Setter
  private Map<String, StatusLogEntry> statusChanges;

  @Column(nullable = false)
  @Getter
  @Setter
  private Boolean emergency;

  @Column(nullable = false)
  @Getter
  @Setter
  private Integer numberOfMonthsInPeriod;

  @Getter
  @Setter
  @Type(type = UUID_TYPE)
  private UUID supervisoryNodeId;

  @ManyToMany
  @JoinTable(name = "requisitions_previous_requisitions",
      joinColumns = {@JoinColumn(name = "requisitionId")},
      inverseJoinColumns = {@JoinColumn(name = "previousRequisitionId")})
  @Getter
  @Setter
  private List<Requisition> previousRequisitions;

  @ElementCollection(fetch = FetchType.EAGER, targetClass = UUID.class)
  @Column(name = "value")
  @CollectionTable(
      name = "available_non_full_supply_products",
      joinColumns = @JoinColumn(name = "requisitionId"))
  @Getter
  @Setter
  @Type(type = UUID_TYPE)
  private Set<UUID> availableNonFullSupplyProducts;

  /**
   * Constructor.
   *
   * @param facilityId         id of the Facility
   * @param programId          id of the Program
   * @param processingPeriodId id of the ProcessingPeriod
   * @param status             status of the Requisition
   * @param emergency          whether this Requisition is emergency
   */
  public Requisition(UUID facilityId, UUID programId, UUID processingPeriodId,
                     RequisitionStatus status, Boolean emergency) {
    this.facilityId = facilityId;
    this.programId = programId;
    this.processingPeriodId = processingPeriodId;
    this.status = status;
    this.emergency = emergency;
  }

  /**
   * Initiates the state of a requisition by creating line items based on products
   *
   * @param template             the requisition template for this requisition to use (based on
   *                             program)
   * @param products             the full supply products for this requisitions facility to build
   *                             requisition lines for
   * @param previousRequisitions the previous requisitions for this program/facility. Used for field
   *                             calculations and set previous adjusted consumptions. Pass empty
   *                             list if there are no previous requisitions.
   */
  public void initiate(RequisitionTemplate template,
                       Collection<FacilityTypeApprovedProduct> products,
                       List<Requisition> previousRequisitions,
                       int numberOfPreviousPeriodsToAverage,
                       ProofOfDelivery proofOfDelivery) {
    this.template = template;
    this.previousRequisitions = previousRequisitions;

    setRequisitionLineItems(
        products
            .stream()
            .map(ftap -> new RequisitionLineItem(this, ftap))
            .collect(Collectors.toList())
    );

    // Firstly, if we display the column ...
    // ... and if the previous requisition exists ...
    if (!previousRequisitions.isEmpty()
        && null != previousRequisitions.get(0)
        && template.isColumnDisplayed(RequisitionLineItem.BEGINNING_BALANCE)) {
      // .. for each line from the current requisition ...
      getNonSkippedFullSupplyRequisitionLineItems().forEach(currentLine -> {
        // ... we try to find line in the previous requisition for the same product ...
        RequisitionLineItem previousLine = previousRequisitions.get(0)
            .findLineByProductId(currentLine.getOrderableId());

        // ... and in the end we use it to calculate beginning balance in a new line.
        currentLine.setBeginningBalance(
            LineItemFieldsCalculator.calculateBeginningBalance(previousLine));
      });
    }

    // Secondly, if Proof Of Delivery exists and it is submitted ...
    if (null != proofOfDelivery && proofOfDelivery.isSubmitted()) {
      // .. for each line from the current requisition ...
      getNonSkippedFullSupplyRequisitionLineItems().forEach(requisitionLine -> {
        // ... we try to find line in POD for the same product ...
        ProofOfDeliveryLineItem proofOfDeliveryLine = proofOfDelivery
            .findLineByProductId(requisitionLine.getOrderableId());

        // ... and if line exists we set value for Total Received Quantity (B) column
        if (null != proofOfDeliveryLine) {
          requisitionLine.setTotalReceivedQuantity(
              (int) zeroIfNull(proofOfDeliveryLine.getQuantityReceived())
          );
        }
      });
    }

    setPreviousAdjustedConsumptions(numberOfPreviousPeriodsToAverage);
  }

  /**
   * Submits given requisition.
   *
   * @param products orderable products that will be used by line items to update packs to ship.
   */
  public void submit(Collection<Orderable> products, UUID submitter) {
    if (!INITIATED.equals(status)) {
      throw new IllegalStateException();
    }

    if (RequisitionHelper.areFieldsNotFilled(template,
        getNonSkippedFullSupplyRequisitionLineItems())) {
      throw new IllegalStateException();
    }

    updateConsumptionsAndTotalCost(products);

    status = RequisitionStatus.SUBMITTED;
  }

  /**
   * Authorize given Requisition.
   *
   * @param products orderable products that will be used by line items to update packs to ship.
   */
  public void authorize(Collection<Orderable> products, UUID authorizer) {
    if (!RequisitionStatus.SUBMITTED.equals(status)) {
      throw new IllegalStateException();
    }

    updateConsumptionsAndTotalCost(products);
    populateApprovedQuantity();

    status = RequisitionStatus.AUTHORIZED;
    RequisitionHelper.forEachLine(getSkippedRequisitionLineItems(), RequisitionLineItem::resetData);
  }

  /**
   * Check if the requisition is approvable.
   */
  public boolean isApprovable() {
    return status.duringApproval();
  }

  /**
   * Approves given requisition.
   *
   * @param parentNodeId supervisoryNodeDto parent node of the supervisoryNode for this
   *                     requisition.
   * @param products     orderable products that will be used by line items to update packs to
   *                     ship.
   */
  public void approve(UUID parentNodeId, Collection<Orderable> products) {
    if (parentNodeId == null) {
      status = RequisitionStatus.APPROVED;
    } else {
      status = RequisitionStatus.IN_APPROVAL;
      supervisoryNodeId = parentNodeId;
    }

    updateConsumptionsAndTotalCost(products);
  }

  /**
   * Rejects given requisition.
   */
  public void reject(Collection<Orderable> products) {
    status = RequisitionStatus.INITIATED;
    updateConsumptionsAndTotalCost(products);
  }

  /**
   * Finds first RequisitionLineItem that have productId property equals to the given productId
   * argument.
   *
   * @param productId UUID of orderable product
   * @return first RequisitionLineItem that have productId property equals to the given productId
   *         argument; otherwise null;
   */
  public RequisitionLineItem findLineByProductId(UUID productId) {
    if (null == requisitionLineItems) {
      return null;
    }

    return requisitionLineItems
        .stream()
        .filter(e -> Objects.equals(productId, e.getOrderableId()))
        .findFirst()
        .orElse(null);
  }

  public void setDraftStatusMessage(String draftStatusMessage) {
    this.draftStatusMessage = (draftStatusMessage == null) ? "" : draftStatusMessage;
  }

  public boolean isPreAuthorize() {
    return status.isPreAuthorize();
  }

  /**
   * Filter out requisitionLineItems that are skipped.
   *
   * @return requisitionLineItems that are not skipped
   */
  public List<RequisitionLineItem> getNonSkippedRequisitionLineItems() {
    if (requisitionLineItems == null) {
      return Collections.emptyList();
    }
    return this.requisitionLineItems.stream()
        .filter(line -> !line.getSkipped())
        .collect(Collectors.toList());
  }

  /**
   * Filter out requisitionLineItems that are skipped and not-full supply.
   *
   * @return non-skipped full supply requisition line items
   */
  public List<RequisitionLineItem> getNonSkippedFullSupplyRequisitionLineItems() {
    if (requisitionLineItems == null) {
      return Collections.emptyList();
    }
    return this.requisitionLineItems.stream()
        .filter(line -> !line.getSkipped())
        .filter(line -> !line.isNonFullSupply())
        .collect(Collectors.toList());
  }

  /**
   * Filter out requisitionLineItems that are skipped and full supply.
   *
   * @return non-skipped non-full supply requisition line items
   */
  public List<RequisitionLineItem> getNonSkippedNonFullSupplyRequisitionLineItems() {
    if (requisitionLineItems == null) {
      return Collections.emptyList();
    }
    return this.requisitionLineItems.stream()
        .filter(line -> !line.getSkipped())
        .filter(RequisitionLineItem::isNonFullSupply)
        .collect(Collectors.toList());
  }

  /**
   * Filter out requisitionLineItems that are not skipped.
   *
   * @return requisitionLineItems that are skipped
   */
  public List<RequisitionLineItem> getSkippedRequisitionLineItems() {
    return this.requisitionLineItems.stream()
        .filter(RequisitionLineItem::getSkipped)
        .collect(Collectors.toList());
  }

  /**
   * Calculates combined cost of all requisition line items.
   *
   * @return sum of total costs.
   */
  public Money getTotalCost() {
    return calculateTotalCostForLines(requisitionLineItems);
  }

  /**
   * Calculates combined cost of non-full supply non-skipped requisition line items.
   *
   * @return sum of total costs.
   */
  public Money getNonFullSupplyTotalCost() {
    return calculateTotalCostForLines(getNonSkippedNonFullSupplyRequisitionLineItems());
  }

  /**
   * Calculates combined cost of full supply non-skipped requisition line items.
   *
   * @return sum of total costs.
   */
  public Money getFullSupplyTotalCost() {
    return calculateTotalCostForLines(getNonSkippedFullSupplyRequisitionLineItems());
  }

  /**
   * Sets appropriate value for Previous Adjusted Consumptions field in
   * each {@link RequisitionLineItem}.
   */
  void setPreviousAdjustedConsumptions(int numberOfPreviousPeriodsToAverage) {
    List<RequisitionLineItem> previousRequisitionLineItems = RequisitionHelper
        .getNonSkippedLineItems(previousRequisitions.subList(0, numberOfPreviousPeriodsToAverage));

    RequisitionHelper.forEachLine(requisitionLineItems,
        line -> {
          List<RequisitionLineItem> withProductId = RequisitionHelper
              .findByProductId(previousRequisitionLineItems, line.getOrderableId());
          List<Integer> adjustedConsumptions = RequisitionHelper
              .mapToAdjustedConsumptions(withProductId);

          line.setPreviousAdjustedConsumptions(adjustedConsumptions);
        });
  }

  private Money calculateTotalCostForLines(List<RequisitionLineItem> requisitionLineItems) {
    Money defaultValue = Money.of(CurrencyUnit.of(CurrencyConfig.CURRENCY_CODE), 0);

    if (requisitionLineItems.isEmpty()) {
      return defaultValue;
    }

    Optional<Money> money = requisitionLineItems.stream()
        .map(RequisitionLineItem::getTotalCost).filter(Objects::nonNull).reduce(Money::plus);

    return money.isPresent() ? money.get() : defaultValue;
  }

  private void calculateAndValidateTemplateFields(
      RequisitionTemplate template, Collection<StockAdjustmentReason> stockAdjustmentReasons) {
    getNonSkippedFullSupplyRequisitionLineItems()
        .forEach(line -> line.calculateAndSetFields(template, stockAdjustmentReasons,
            numberOfMonthsInPeriod));
  }

  private void updateConsumptionsAndTotalCost(Collection<Orderable> products) {
    getNonSkippedRequisitionLineItems().forEach(line -> line.updatePacksToShip(products));

    if (template.isColumnInTemplate(ADJUSTED_CONSUMPTION)) {
      getNonSkippedFullSupplyRequisitionLineItems().forEach(line -> line.setAdjustedConsumption(
          LineItemFieldsCalculator.calculateAdjustedConsumption(line, numberOfMonthsInPeriod)
      ));
    }

    if (template.isColumnInTemplate(AVERAGE_CONSUMPTION)) {
      getNonSkippedFullSupplyRequisitionLineItems().forEach(
          RequisitionLineItem::calculateAndSetAverageConsumption);
    }

    getNonSkippedRequisitionLineItems().forEach(line -> line.setTotalCost(
        LineItemFieldsCalculator.calculateTotalCost(line,
            CurrencyUnit.of(CurrencyConfig.CURRENCY_CODE))
    ));
  }

  private void populateApprovedQuantity() {
    if (template.isColumnDisplayed(CALCULATED_ORDER_QUANTITY)) {
      getNonSkippedRequisitionLineItems().forEach(line -> {
        if (isNull(line.getRequestedQuantity())) {
          line.setApprovedQuantity(line.getCalculatedOrderQuantity());
        } else {
          line.setApprovedQuantity(line.getRequestedQuantity());
        }
      });
    } else {
      getNonSkippedRequisitionLineItems().forEach(line ->
          line.setApprovedQuantity(line.getRequestedQuantity())
      );
    }
  }

  private void updateReqLines(Collection<RequisitionLineItem> newLineItems) {
    if (null == newLineItems) {
      return;
    }

    if (null == requisitionLineItems) {
      requisitionLineItems = new ArrayList<>();
    }

    List<RequisitionLineItem> updatedList = new ArrayList<>();

    for (RequisitionLineItem item : newLineItems) {
      RequisitionLineItem existing = requisitionLineItems
          .stream()
          .filter(l -> l.getId().equals(item.getId()))
          .findFirst().orElse(null);

      if (null == existing) {
        if (item.isNonFullSupply()) {
          item.setRequisition(this);
          updatedList.add(item);
        }
      } else {
        existing.setRequisition(this);
        existing.updateFrom(item);
        updatedList.add(existing);
      }
    }

    // is there a full supply line that is not in update list
    // it should be added. Those lines should not be removed
    // during update. Only non full supply lines can be
    // added/updated/removed.
    requisitionLineItems
        .stream()
        .filter(line -> !line.isNonFullSupply())
        .filter(line -> updatedList
            .stream()
            .map(BaseEntity::getId)
            .noneMatch(id -> Objects.equals(id, line.getId()))
        )
        .forEach(updatedList::add);

    requisitionLineItems.clear();
    requisitionLineItems.addAll(updatedList);
  }

}
