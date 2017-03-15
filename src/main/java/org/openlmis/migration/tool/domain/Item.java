package org.openlmis.migration.tool.domain;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

@Getter
@Setter
@ToString
@NoArgsConstructor
@Entity(name = "CTF_Item")
public class Item implements Serializable {
  private static final long serialVersionUID = 2303684107384909148L;

  @Id
  @Column(name = "ci_ctrItemID")
  private Integer id;

  @OneToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "Fac_Code", referencedColumnName = "Fac_Code")
  private Facility facility;

  @Column(name = "P_Date")
  private LocalDateTime processingDate;

  @Column(name = "Prod_Name")
  private String productName;

  @Column(name = "Prod_Dose")
  private String productDose;

  @OneToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "lngCatProductID")
  private CategoryProductJoin categoryProduct;

  @Column(name = "lngProductID")
  private Integer product;

  @Column(name = "Open_Bal")
  private Integer openingBalance;

  @Column(name = "Receipts")
  private Integer receipts;

  @Column(name = "Issues")
  private Integer issuedQuantity;

  @Column(name = "Dispensed")
  private Integer dispensedQuantity;

  @Column(name = "Adjustments")
  private Integer adjustmentCount;

  @OneToMany(fetch = FetchType.EAGER, mappedBy = "item")
  @Fetch(FetchMode.SELECT)
  private List<Adjustment> adjustments;

  @OneToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "Adj_Type")
  private AdjustmentType adjustmentType;

  @Column(name = "Closing_Bal")
  private Integer closingBalance;

  @Column(name = "6MO_Balance")
  private Integer sixMoBalance;

  @Column(name = "Err_OB")
  private Boolean errorOpeningBalance;

  @Column(name = "Err_CB")
  private Boolean errorClosingBalance;

  @Column(name = "DE_Stat")
  private Short dataEntryStatus;

  @Column(name = "New_Visits")
  private Integer newVisits;

  @Column(name = "Cont_Visits")
  private Integer continuingVisits;

  @Column(name = "Err_qty_rqrd")
  private Boolean errorRequiredQuantity;

  @Column(name = "Err_qty_rcvd")
  private Boolean errorReceivedQuantity;

  @Column(name = "Err_AMC")
  private Boolean errorAverageMonthlyConsumption;

  @Column(name = "Avg_mnthly_cons")
  private Integer averageMonthlyConsumption;

  @Column(name = "Qty_required")
  private Integer requiredQuantity;

  @Column(name = "Qty_received")
  private Integer receivedQuantity;

  @Column(name = "txtNotes")
  private String notes;

  @Column(name = "Qty_OnOrder")
  private Integer onOrderQuantity;

  @Column(name = "Err_qty_DispVsPurpose")
  private Boolean errorDispensedQuantityVsPurpose;

  @Column(name = "Qty_OnHand")
  private Integer onHandQuantity;

  @Column(name = "fStockedOut")
  private Boolean productStockedOut;

  @Column(name = "Qty_RequiredCalc")
  private Integer calculatedRequiredQuantity;

  @Column(name = "intStockedOutDays")
  private Short stockedOutDays;

  @Column(name = "DispensedAdj")
  private Integer adjustedDispensedQuantity;

  @Column(name = "qtySupplierIssued")
  private Integer supplierIssuedQuantity;

  @Column(name = "lngQtyMaxStock")
  private Integer maxStockQuantity;

  @Column(name = "ERR_MaxStock")
  private Boolean errorMaxStockQuantity;

  @OneToMany(fetch = FetchType.EAGER, mappedBy = "item")
  @Fetch(FetchMode.SELECT)
  private List<Purpose> purposes;

}
