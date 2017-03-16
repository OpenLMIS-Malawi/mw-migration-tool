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
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Transient;

@Entity
@DiscriminatorValue("ftp")
@NoArgsConstructor
public class FtpTransferProperties extends TransferProperties {

  @Column(columnDefinition = TEXT_COLUMN_DEFINITION)
  @Enumerated(EnumType.STRING)
  @Getter
  @Setter
  private FtpProtocol protocol;

  @Column(columnDefinition = TEXT_COLUMN_DEFINITION)
  @Getter
  @Setter
  private String username;

  @Column(columnDefinition = TEXT_COLUMN_DEFINITION)
  @Getter
  @Setter
  private String password;

  @Column(columnDefinition = TEXT_COLUMN_DEFINITION)
  @Getter
  @Setter
  private String serverHost;

  @Column
  @Getter
  @Setter
  private Integer serverPort;

  @Column(columnDefinition = TEXT_COLUMN_DEFINITION)
  @Getter
  @Setter
  private String remoteDirectory;

  @Column(columnDefinition = TEXT_COLUMN_DEFINITION)
  @Getter
  @Setter
  private String localDirectory;

  @Column
  @Getter
  @Setter
  private Boolean passiveMode;

  @Override
  @Transient
  public String getPath() {
    return localDirectory;
  }

}
