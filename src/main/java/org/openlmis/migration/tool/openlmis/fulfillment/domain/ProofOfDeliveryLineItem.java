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

package org.openlmis.migration.tool.openlmis.fulfillment.domain;

import org.openlmis.migration.tool.openlmis.BaseEntity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@NoArgsConstructor
@Table(name = "proof_of_delivery_line_items")
public class ProofOfDeliveryLineItem extends BaseEntity {
  public static final String QUANTITY_RECEIVED = "quantityReceived";

  @OneToOne
  @JoinColumn(name = "orderLineItemId", nullable = false)
  @Getter
  @Setter
  private OrderLineItem orderLineItem;

  @ManyToOne(cascade = CascadeType.REFRESH)
  @JoinColumn(name = "proofOfDeliveryId", nullable = false)
  @Getter
  @Setter
  private ProofOfDelivery proofOfDelivery;

  @Getter
  @Setter
  private Long quantityShipped;

  @Getter
  @Setter
  private Long quantityReceived;

  @Getter
  @Setter
  private Long quantityReturned;

  @Column(columnDefinition = TEXT_COLUMN_DEFINITION)
  @Getter
  @Setter
  private String replacedProductCode;

  @Column(columnDefinition = TEXT_COLUMN_DEFINITION)
  @Getter
  @Setter
  private String notes;

  public ProofOfDeliveryLineItem(ProofOfDelivery parent, OrderLineItem item) {
    this.proofOfDelivery = parent;
    this.orderLineItem = item;
  }

  /**
   * Copy values of attributes into new or updated ProofOfDeliveryLineItem.
   *
   * @param proofOfDeliveryLineItem ProofOfDeliveryLineItem with new values.
   */
  public void updateFrom(ProofOfDeliveryLineItem proofOfDeliveryLineItem) {
    this.quantityReceived = proofOfDeliveryLineItem.getQuantityReceived();
    this.quantityReturned = proofOfDeliveryLineItem.getQuantityReturned();
    this.notes = proofOfDeliveryLineItem.getNotes();
  }

}
