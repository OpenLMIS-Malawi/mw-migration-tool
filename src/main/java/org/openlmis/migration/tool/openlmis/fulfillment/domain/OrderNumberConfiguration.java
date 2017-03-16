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
import org.openlmis.migration.tool.openlmis.referencedata.domain.Program;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "order_number_configurations")
@EqualsAndHashCode(callSuper = false)
public class OrderNumberConfiguration extends BaseEntity {

  @Getter
  @Setter
  @Column
  private String orderNumberPrefix;

  @Getter
  @Setter
  @Column(nullable = false)
  private Boolean includeOrderNumberPrefix;

  @Getter
  @Setter
  @Column(nullable = false)
  private Boolean includeProgramCode;

  @Getter
  @Setter
  @Column(nullable = false)
  private Boolean includeTypeSuffix;

  /**
   * Generates order number for given parameters.
   *
   * @param order   order instance.
   * @param program Program associated with the order.
   * @return Generated orderNumber.
   * @throws OrderNumberException if the order parameter is {@code null}
   */
  public String generateOrderNumber(Order order, Program program) {
    if (order == null) {
      throw new OrderNumberException("Order cannot be empty");
    }

    StringBuilder orderNumber = new StringBuilder();

    if (includeOrderNumberPrefix && orderNumberPrefix != null) {
      orderNumber.append(getOrderNumberPrefix());
    }

    if (includeProgramCode && program != null) {
      orderNumber.append(getTruncatedProgramCode(program.getCode().toString()));
    }

    orderNumber.append(order.getExternalId().toString());

    if (includeTypeSuffix) {
      orderNumber.append(order.getEmergency() ? "E" : "R");
    }

    return orderNumber.toString();
  }

  private String getTruncatedProgramCode(String code) {
    return code.length() > 35 ? code.substring(0, 35) : code;
  }

  /**
   * Create new instance of OrderNumberConfiguration based on given
   * {@link Importer}
   * @param importer instance of {@link Importer}
   * @return instance of OrderNumberConfiguration.
   */
  public static OrderNumberConfiguration newInstance(Importer importer) {
    OrderNumberConfiguration orderNumberConfiguration = new OrderNumberConfiguration();
    orderNumberConfiguration.setId(importer.getId());
    orderNumberConfiguration.setIncludeOrderNumberPrefix(importer.getIncludeOrderNumberPrefix());
    orderNumberConfiguration.setIncludeProgramCode(importer.getIncludeProgramCode());
    orderNumberConfiguration.setOrderNumberPrefix(importer.getOrderNumberPrefix());
    orderNumberConfiguration.setIncludeTypeSuffix(importer.getIncludeTypeSuffix());

    return orderNumberConfiguration;
  }

  /**
   * Export this object to the specified exporter (DTO).
   *
   * @param exporter exporter to export to
   */
  public void export(Exporter exporter) {
    exporter.setId(id);
    exporter.setIncludeOrderNumberPrefix(includeOrderNumberPrefix);
    exporter.setIncludeProgramCode(includeProgramCode);
    exporter.setOrderNumberPrefix(orderNumberPrefix);
    exporter.setIncludeTypeSuffix(includeTypeSuffix);
  }

  public interface Exporter {
    void setId(UUID id);

    void setOrderNumberPrefix(String orderNumberPrefix);

    void setIncludeOrderNumberPrefix(Boolean includeOrderNumberPrefix);

    void setIncludeProgramCode(Boolean includeProgramCode);

    void setIncludeTypeSuffix(Boolean includeTypeSuffix);

  }

  public interface Importer {
    UUID getId();

    String getOrderNumberPrefix();

    Boolean getIncludeOrderNumberPrefix();

    Boolean getIncludeProgramCode();

    Boolean getIncludeTypeSuffix();

  }
}
