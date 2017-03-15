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
@Entity(name = "CTF_Comments")
public class Comment implements Serializable {
  private static final long serialVersionUID = 6477006107959837197L;

  @Id
  @Column(name = "ctfc_lngID")
  private Integer id;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "ctf_ItemID")
  private Item item;

  @OneToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "Com_Code")
  private CommentType type;

  @Column(name = "ctfc_txt")
  private String comment;

}
