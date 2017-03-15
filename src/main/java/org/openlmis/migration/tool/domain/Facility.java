package org.openlmis.migration.tool.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

@Getter
@Setter
@ToString
@NoArgsConstructor
@Entity(name = "Facility")
public class Facility implements Serializable {
  private static final long serialVersionUID = 1885948060532493308L;

  @Id
  @Column(name = "Fac_Code")
  private String code;

  @Column(name = "Fac_Name")
  private String name;

  @OneToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "Fac_Type")
  private FacilityType type;

  @Column(name = "Sup_Code")
  private String supplyingCode;

  @Column(name = "Program")
  private String program;

  @Column(name = "Fac_Contact")
  private String contact;

  @Column(name = "Fac_Address1")
  private String address1;

  @Column(name = "Fac_Address2")
  private String address2;

  @Column(name = "Fac_City")
  private String city;

  @Column(name = "Fac_State")
  private String state;

  @Column(name = "Fac_Rescode")
  private String rescode;

  @Column(name = "Fac_Phone")
  private String phone;

  @Column(name = "Fac_FAX")
  private String fax;

  @Column(name = "Field_Dispense")
  private Boolean dispense;

  @Column(name = "MaxMOS")
  private Double maxMonthsOfSupply;

  @Column(name = "MinMOS")
  private Double minMonthsOfSupply;

  @Column(name = "Active")
  private Boolean active;

  @Column(name = "Warehouse")
  private Boolean warehouse;

  @Column(name = "Highest_LV")
  private Boolean highestLevel;

  @OneToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "Distribution_Role")
  private DistributionLevel role;

  @OneToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "dc_GuidID", columnDefinition = "character")
  private DataCenter dataCenter;

  @Column(name = "fac_guidId", columnDefinition = "character")
  private String id;

}
