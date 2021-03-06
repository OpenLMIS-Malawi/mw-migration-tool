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

package mw.gov.health.lmis.migration.tool.openlmis.fulfillment.domain;

import static org.springframework.util.CollectionUtils.isEmpty;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Type;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mw.gov.health.lmis.migration.tool.openlmis.BaseEntity;
import mw.gov.health.lmis.migration.tool.openlmis.referencedata.domain.User;
import mw.gov.health.lmis.migration.tool.openlmis.requisition.domain.Requisition;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;

@Entity
@Table(name = "orders", schema = "fulfillment")
@NoArgsConstructor
public class Order extends BaseEntity {
  public static final String SUPPLYING_FACILITY_ID = "supplyingFacilityId";
  public static final String REQUESTING_FACILITY_ID = "requestingFacilityId";
  public static final String PROGRAM_ID = "programId";
  public static final String STATUS = "status";
  public static final String PROCESSING_PERIOD_ID = "processingPeriodId";

  @Column(nullable = false, unique = true)
  @Getter
  @Setter
  @Type(type = UUID_TYPE)
  private UUID externalId;

  @Column(nullable = false)
  @Getter
  @Setter
  private Boolean emergency;

  @Getter
  @Setter
  @Type(type = UUID_TYPE)
  private UUID facilityId;

  @Getter
  @Setter
  @Type(type = UUID_TYPE)
  private UUID processingPeriodId;

  @Getter
  @Setter
  @Column(columnDefinition = "timestamp with time zone")
  private ZonedDateTime createdDate;

  @Column(nullable = false)
  @Getter
  @Setter
  @Type(type = UUID_TYPE)
  private UUID createdById;

  @Column(nullable = false)
  @Getter
  @Setter
  @Type(type = UUID_TYPE)
  private UUID programId;

  @Column(nullable = false)
  @Getter
  @Setter
  @Type(type = UUID_TYPE)
  private UUID requestingFacilityId;

  @Column(nullable = false)
  @Getter
  @Setter
  @Type(type = UUID_TYPE)
  private UUID receivingFacilityId;

  @Column(nullable = false)
  @Getter
  @Setter
  @Type(type = UUID_TYPE)
  private UUID supplyingFacilityId;

  @Column(nullable = false, unique = true, columnDefinition = TEXT_COLUMN_DEFINITION)
  @Getter
  @Setter
  private String orderCode;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  @Getter
  @Setter
  private OrderStatus status;

  @Column(nullable = false)
  @Getter
  @Setter
  private BigDecimal quotedCost;

  @OneToMany(
      mappedBy = "order",
      cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH, CascadeType.REMOVE},
      fetch = FetchType.EAGER,
      orphanRemoval = true)
  @Fetch(FetchMode.SELECT)
  @Getter
  @Setter
  private List<OrderLineItem> orderLineItems;

  @OneToMany(
      mappedBy = "order",
      cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH, CascadeType.REMOVE},
      fetch = FetchType.EAGER,
      orphanRemoval = true)
  @Fetch(FetchMode.SELECT)
  @Getter
  @Setter
  private List<StatusMessage> statusMessages;

  @OneToMany(
      mappedBy = "order",
      cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH, CascadeType.REMOVE},
      fetch = FetchType.EAGER,
      orphanRemoval = true)
  @Getter
  @Setter
  private List<StatusChange> statusChanges;


  @PrePersist
  private void prePersist() {
    this.createdDate = ZonedDateTime.now();
    forEachLine(line -> line.setOrder(this));
    forEachStatus(status -> status.setOrder(this));
    forEachStatusChange(change -> change.setOrder(this));
  }

  @PreUpdate
  private void preUpdate() {
    forEachLine(line -> line.setOrder(this));
    forEachStatus(status -> status.setOrder(this));
    forEachStatusChange(change -> change.setOrder(this));
  }

  /**
   * Copy values of attributes into new or updated Order.
   *
   * @param order Order with new values.
   */
  public void updateFrom(Order order) {
    this.externalId = order.externalId;
    this.emergency = order.emergency;
    this.facilityId = order.facilityId;
    this.processingPeriodId = order.processingPeriodId;
    this.createdById = order.createdById;
    this.programId = order.programId;
    this.requestingFacilityId = order.requestingFacilityId;
    this.receivingFacilityId = order.receivingFacilityId;
    this.supplyingFacilityId = order.supplyingFacilityId;
    this.orderCode = order.orderCode;
    this.status = order.status;
    this.quotedCost = order.quotedCost;
    this.statusChanges = order.statusChanges;
  }

  public void forEachLine(Consumer<OrderLineItem> consumer) {
    Optional.ofNullable(orderLineItems)
        .ifPresent(list -> list.forEach(consumer));
  }

  public void forEachStatus(Consumer<StatusMessage> consumer) {
    Optional.ofNullable(statusMessages)
        .ifPresent(list -> list.forEach(consumer));
  }

  public void forEachStatusChange(Consumer<StatusChange> consumer) {
    Optional.ofNullable(statusChanges)
            .ifPresent(list -> list.forEach(consumer));
  }

  /**
   * Creates new order instance based on requisition.
   */
  public static Order newOrder(Requisition requisition, User user) {
    if (null == requisition) {
      return null;
    }

    Order order = new Order();
    order.setExternalId(requisition.getId());
    order.setEmergency(requisition.getEmergency());
    order.setFacilityId(requisition.getFacilityId());
    order.setProcessingPeriodId(requisition.getProcessingPeriodId());
    order.setQuotedCost(BigDecimal.ZERO);

    order.setReceivingFacilityId(requisition.getFacilityId());
    order.setRequestingFacilityId(requisition.getFacilityId());

    order.setSupplyingFacilityId(requisition.getSupplyingFacilityId());
    order.setProgramId(requisition.getProgramId());

    if (!isEmpty(requisition.getStatusMessages())) {
      order.setStatusMessages(
          requisition
              .getStatusMessages()
              .stream()
              .map(msg -> new StatusMessage(msg.getAuthorId(), msg.getStatus(), msg.getBody()))
              .collect(Collectors.toList())
      );
    }

    order.setOrderLineItems(
        requisition
            .getRequisitionLineItems()
            .stream()
            .filter(line -> !line.getSkipped())
            .map(OrderLineItem::newOrderLineItem)
            .collect(Collectors.toList())
    );

    if (!isEmpty(requisition.getStatusChanges())) {
      order.setStatusChanges(
          requisition
              .getStatusChanges()
              .stream()
              .map(sc -> new StatusChange(sc.getStatus(), sc.getAuthorId(), sc.getCreatedDate()))
              .collect(Collectors.toList())
      );
    }

    order.setCreatedById(user.getId());

    return order;
  }

}
