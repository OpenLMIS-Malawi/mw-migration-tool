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

package org.openlmis.migration.tool.openlmis.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProofOfDeliveryDto {
  private UUID id;
  private OrderDto order;
  private List<ProofOfDeliveryLineItemDto> proofOfDeliveryLineItems;
  private String deliveredBy;
  private String receivedBy;
  private ZonedDateTime receivedDate;

  @JsonIgnore
  public boolean isSubmitted() {
    return null != order && OrderStatus.RECEIVED == order.getStatus();
  }

  /**
   * Finds a correct line item based on product id.
   */
  public ProofOfDeliveryLineItemDto findLineByProductId(UUID productId) {
    if (null == proofOfDeliveryLineItems) {
      return null;
    }

    return proofOfDeliveryLineItems
        .stream()
        .filter(e -> null != e.getOrderLineItem() && null != e.getOrderLineItem().getOrderable())
        .filter(e -> Objects.equals(productId, e.getOrderLineItem().getOrderable().getId()))
        .findFirst()
        .orElse(null);
  }
}
