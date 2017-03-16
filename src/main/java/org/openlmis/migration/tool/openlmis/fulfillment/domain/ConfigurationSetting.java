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

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "configuration_settings")
@NoArgsConstructor
public class ConfigurationSetting {

  @Id
  @Getter
  @Setter
  private String key;

  @Column(nullable = false)
  @Getter
  @Setter
  private String value;

  /**
   * Creates a new instance of {@link ConfigurationSetting} based on data from dto.
   *
   * @param importer configuration settings.
   * @return new instance of {@link ConfigurationSetting}.
   */
  public static ConfigurationSetting newInstance(Importer importer) {
    ConfigurationSetting setting = new ConfigurationSetting();
    setting.setKey(importer.getKey());
    setting.setValue(importer.getValue());

    return setting;
  }

  /**
   * Exports current state of this configuration setting.
   *
   * @param exporter an instance of {@link Exporter}
   */
  public void export(Exporter exporter) {
    exporter.setKey(key);
    exporter.setValue(value);
  }

  public interface Importer {

    String getKey();

    String getValue();

  }

  public interface Exporter {

    void setKey(String key);

    void setValue(String value);

  }

}
