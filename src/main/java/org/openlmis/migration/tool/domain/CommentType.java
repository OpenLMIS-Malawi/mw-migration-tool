package org.openlmis.migration.tool.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Getter
@Setter
@ToString
@NoArgsConstructor
@Entity(name = "Comment_Type")
public class CommentType implements Serializable {
  private static final long serialVersionUID = 991633996839159483L;

  @Id
  @Column(name = "Com_Code")
  private String code;

  @Column(name = "Com_Name")
  private String name;

  @Column(name = "Com_Active")
  private Boolean active;

  @Column(name = "Com_Order")
  private Short order;

  @Column(name = "Com_GuidID")
  private String id;

}
