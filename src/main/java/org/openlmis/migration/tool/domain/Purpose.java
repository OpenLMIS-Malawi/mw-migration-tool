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
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;

@Getter
@Setter
@ToString
@NoArgsConstructor
@Entity(name = "CTF_Purpose")
public class Purpose implements Serializable {
  private static final long serialVersionUID = -7988284031869855018L;

  @Id
  @Column(name = "ctp_lngID")
  private Integer id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ctf_ItemID")
  private Item item;

  @OneToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "pur_lngID")
  private PurposeOfUse purposeOfUse;

  @Column(name = "ctp_lngQty")
  private Integer quantity;

}
