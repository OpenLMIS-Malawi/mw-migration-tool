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

package mw.gov.health.lmis.migration.tool.openlmis.requisition.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mw.gov.health.lmis.migration.tool.openlmis.BaseEntity;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;

/**
 * Defines a parameter of Jasper Template, meant to be passed on printing.
 * <p>
 * selectExpression is used to indicate how the parameter values should be retrieved (API path).
 * selectProperty indicates which property of objects should be passed as parameter (ex. ID).
 * displayProperty is used to indicate which property of object should be displayed for choice.
 * </p>
 */
@Entity
@Table(name = "template_parameters", schema = "requisition")
@NoArgsConstructor
public class JasperTemplateParameter extends BaseEntity {

  @ManyToOne(cascade = CascadeType.REFRESH)
  @JoinColumn(name = "templateId", nullable = false)
  @Getter
  @Setter
  private JasperTemplate template;

  @Column(columnDefinition = TEXT_COLUMN_DEFINITION)
  @Getter
  @Setter
  private String name;

  @Column(columnDefinition = TEXT_COLUMN_DEFINITION)
  @Getter
  @Setter
  private String displayName;

  @Column(columnDefinition = TEXT_COLUMN_DEFINITION)
  @Getter
  @Setter
  private String defaultValue;

  @Column(columnDefinition = TEXT_COLUMN_DEFINITION)
  @Getter
  @Setter
  private String dataType;

  @Column(columnDefinition = TEXT_COLUMN_DEFINITION)
  @Getter
  @Setter
  private String selectExpression;

  @Column(columnDefinition = TEXT_COLUMN_DEFINITION)
  @Getter
  @Setter
  private String selectMethod;

  @Column(columnDefinition = TEXT_COLUMN_DEFINITION)
  @Getter
  @Setter
  private String selectBody;

  @Column(columnDefinition = TEXT_COLUMN_DEFINITION)
  @Getter
  @Setter
  private String selectProperty;

  @Column(columnDefinition = TEXT_COLUMN_DEFINITION)
  @Getter
  @Setter
  private String displayProperty;

  @Column(columnDefinition = TEXT_COLUMN_DEFINITION)
  @Getter
  @Setter
  private String description;

  @ElementCollection
  @CollectionTable(
      name = "JasperTemplateParameter_options",
      schema = "requisition",
      joinColumns = @JoinColumn(name = "jaspertemplateparameterid"))
  @Column(name = "options")
  @Setter
  @Getter
  private List<String> options;

  @OneToMany(
      mappedBy = "parameter",
      cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH, CascadeType.REMOVE},
      fetch = FetchType.EAGER,
      orphanRemoval = true)
  @Getter
  @Setter
  private List<JasperTemplateParameterDependency> dependencies;

  @Column
  @Getter
  @Setter
  private Boolean required;

  @PrePersist
  @PreUpdate
  private void preSave() {
    if (dependencies != null) {
      dependencies.forEach(dependency -> dependency.setParameter(this));
    }
  }

}
