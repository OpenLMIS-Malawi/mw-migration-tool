package org.openlmis.migration.tool.domain;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

@Getter
@Setter
@ToString
@NoArgsConstructor
@Entity(name = "CTF_Main")
public class Main implements Serializable  {
  private static final long serialVersionUID = 4758627456202556181L;

  @EmbeddedId
  private ComplexId id;

  @Column(name = "DE_Date")
  private LocalDateTime createdDate;

  @Column(name = "DE_Staff")
  private String createdBy;

  @Column(name = "LC_Date")
  private LocalDateTime modifiedDate;

  @Column(name = "LC_Staff")
  private String modifiedBy;

  @Column(name = "CTF_Stat")
  private Short entered;

  @Column(name = "CTF_Comment")
  private String notes;

  @Column(name = "NoStock_Visits")
  private Integer noStockVisits;

  @Column(name = "dtmReceived")
  private LocalDateTime receivedDate;

  @Column(name = "dtmShipment")
  private LocalDateTime shipmentReceivedData;

  @Column(name = "ctf_lngCasesNew")
  private Integer newCases;

  @Column(name = "ctf_lngCasesRetreatments")
  private Integer retreatmentCases;

  @Column(name = "ctf_lngCasesPediatric")
  private Integer pediatricCases;

  @OneToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "dtl_lngID")
  private DataTransferLog logEntry;

  @Column(name = "dtmPrepared")
  private LocalDateTime preparedDate;

  @Embeddable
  @Getter
  @Setter
  @ToString
  @NoArgsConstructor
  @AllArgsConstructor
  @EqualsAndHashCode
  public static class ComplexId implements Serializable {


    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "Fac_Code", referencedColumnName = "Fac_Code")
    private Facility facility;

    @Column(name = "P_Date")
    private LocalDateTime processingDate;

  }

}
