package org.openlmis.migration.tool.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Getter
@Setter
@NoArgsConstructor
@Entity(name = "Adj_Type")
public class AdjustmentType implements Serializable {
  private static final long serialVersionUID = -3622210331912100854L;

  @Id
  @Column(name = "Type_Code")
  private String code;

  @Column(name = "Type_Name")
  private String name;

  @Column(name = "Always_Negative")
  private Boolean negative;

  @Column(name = "UserCanEnter")
  private Boolean userCanEnter;

  @Column(name = "Adj_Active")
  private Boolean active;

  @Column(name = "adj_guidId", columnDefinition = "character")
  private String id;

  @Override
  public String toString() {
    return code;
  }
}
