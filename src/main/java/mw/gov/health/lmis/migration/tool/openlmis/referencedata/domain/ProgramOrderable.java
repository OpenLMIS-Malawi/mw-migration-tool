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

package mw.gov.health.lmis.migration.tool.openlmis.referencedata.domain;

import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mw.gov.health.lmis.migration.tool.openlmis.BaseEntity;
import mw.gov.health.lmis.migration.tool.openlmis.CurrencyConfig;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "program_orderables", schema = "referencedata")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ProgramOrderable extends BaseEntity {

  @ManyToOne
  @JoinColumn(name = "programId", nullable = false)
  private Program program;

  @ManyToOne
  @JoinColumn(name = "orderableId", nullable = false)
  private Orderable product;

  private Integer dosesPerPatient;

  private boolean active;

  @ManyToOne
  @JoinColumn(name = "orderableDisplayCategoryId", nullable = false)
  private OrderableDisplayCategory orderableDisplayCategory;

  private boolean fullSupply;
  private int displayOrder;

  @Type(type = "org.jadira.usertype.moneyandcurrency.joda.PersistentMoneyAmount",
      parameters = {@Parameter(name = "currencyCode", value = CurrencyConfig.CURRENCY_CODE)})
  private Money pricePerPack;

  private ProgramOrderable(Program program,
                           Orderable product,
                           OrderableDisplayCategory orderableDisplayCategory) {
    this.program = program;
    this.product = product;
    this.orderableDisplayCategory = orderableDisplayCategory;
    this.dosesPerPatient = null;
    this.active = true;
    this.fullSupply = true;
    this.displayOrder = 0;
  }

  /**
   * Returns true if this association is for given Program.
   * @param program the {@link Program} to ask about
   * @return true if this association is for the given Program, false otherwise.
   */
  public boolean isForProgram(Program program) {
    return this.program.equals(program);
  }

  /**
   * Create program orderable association.
   * See {@link #createNew(Program,
   *  OrderableDisplayCategory,
   *  Orderable,
   *  Integer,
   *  boolean,
   *  boolean,
   *  int,
   *  Money,
   *  CurrencyUnit)}.
   * Uses sensible defaults.
   * @param program see other
   * @param category see other
   * @param product see other
   * @return see other
   */
  public static final ProgramOrderable createNew(Program program,
                                                 OrderableDisplayCategory category,
                                                 Orderable product,
                                                 CurrencyUnit currencyUnit) {
    ProgramOrderable programOrderable = new ProgramOrderable(program, product, category);
    programOrderable.pricePerPack = Money.of(currencyUnit, BigDecimal.ZERO);
    return programOrderable;
  }

  /**
   * Create program orderable.
   * @param program The Program this Product will be in.
   * @param category the category this Product will be in, in this Program.
   * @param product the Product.
   * @param dosesPerPatient the number of doses a patient needs of this orderable.
   * @param active weather this orderable is active in this program at this time.
   * @param displayOrder the display order of this Product in this category of this Program.
   * @param pricePerPack the price of one pack.
   * @return a new ProgramOrderable.
   */
  public static final ProgramOrderable createNew(Program program,
                                                 OrderableDisplayCategory category,
                                                 Orderable product,
                                                 Integer dosesPerPatient,
                                                 boolean active,
                                                 boolean fullSupply,
                                                 int displayOrder,
                                                 Money pricePerPack,
                                                 CurrencyUnit currencyUnit) {
    ProgramOrderable programOrderable = createNew(program, category, product, currencyUnit);
    programOrderable.dosesPerPatient = dosesPerPatient;
    programOrderable.active = active;
    programOrderable.fullSupply = fullSupply;
    programOrderable.displayOrder = displayOrder;
    if (pricePerPack != null) {
      programOrderable.pricePerPack = pricePerPack;
    }
    return programOrderable;
  }

  /**
   * Equal if both represent association between same Program and Product.  e.g. Ibuprofen in the
   * Essential Meds Program is always the same association regardless of the other properties.
   * @param other the other ProgramOrderable
   * @return true if for same Program-Orderable association, false otherwise.
   */
  @Override
  public boolean equals(Object other) {
    if (Objects.isNull(other) || !(other instanceof ProgramOrderable)) {
      return false;
    }

    ProgramOrderable otherProgProduct = (ProgramOrderable) other;
    return program.equals(otherProgProduct.program) && product.equals(otherProgProduct.product);
  }

  @Override
  public int hashCode() {
    return Objects.hash(program, product);
  }

  /**
   * Creates new instance based on data from {@link Importer}
   *
   * @param importer instance of {@link Importer}
   * @return new instance of ProgramOrderable.
   */
  public static ProgramOrderable newInstance(Importer importer, Orderable orderable) {
    ProgramOrderable programOrderable = new ProgramOrderable();
    programOrderable.orderableDisplayCategory =
        new OrderableDisplayCategory(importer.getOrderableDisplayCategoryId());
    programOrderable.active = importer.isActive();
    programOrderable.fullSupply = importer.isFullSupply();
    programOrderable.displayOrder = importer.getDisplayOrder();
    programOrderable.dosesPerPatient = importer.getDosesPerPatient();
    programOrderable.pricePerPack = importer.getPricePerPack();
    programOrderable.program = new Program(importer.getProgramId());
    programOrderable.product = orderable;
    return programOrderable;
  }

  /**
   * Export this object to the specified exporter (DTO).
   *
   * @param exporter exporter to export to
   */
  public void export(Exporter exporter) {
    exporter.setOrderableDisplayCategoryId(
        orderableDisplayCategory.getId());
    if (orderableDisplayCategory.getOrderedDisplayValue() != null) {
      exporter.setOrderableCategoryDisplayName(
          orderableDisplayCategory.getOrderedDisplayValue().getDisplayName());
      exporter.setOrderableCategoryDisplayOrder(
          orderableDisplayCategory.getOrderedDisplayValue().getDisplayOrder());
    }
    exporter.setProgramId(program.getId());
    exporter.setActive(active);
    exporter.setFullSupply(fullSupply);
    exporter.setDisplayOrder(displayOrder);
    exporter.setDosesPerPatient(dosesPerPatient);
    if (pricePerPack != null) {
      exporter.setPricePerPack(pricePerPack);
    }

  }

  public interface Exporter {
    void setProgramId(UUID program);

    void setOrderableDisplayCategoryId(UUID category);

    void setOrderableCategoryDisplayName(String name);

    void setOrderableCategoryDisplayOrder(Integer displayOrder);

    void setActive(boolean active);

    void setFullSupply(boolean fullSupply);

    void setDisplayOrder(int displayOrder);

    void setDosesPerPatient(Integer dosesPerPatient);

    void setPricePerPack(Money pricePerPack);
  }

  public interface Importer {
    UUID getProgramId();

    UUID getOrderableDisplayCategoryId();

    boolean isActive();

    boolean isFullSupply();

    int getDisplayOrder();

    Integer getDosesPerPatient();

    Money getPricePerPack();
  }
}
