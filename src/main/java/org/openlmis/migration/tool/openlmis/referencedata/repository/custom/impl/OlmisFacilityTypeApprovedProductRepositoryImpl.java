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

package org.openlmis.migration.tool.openlmis.referencedata.repository.custom.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import org.openlmis.migration.tool.openlmis.referencedata.domain.Facility;
import org.openlmis.migration.tool.openlmis.referencedata.domain.FacilityType;
import org.openlmis.migration.tool.openlmis.referencedata.domain.FacilityTypeApprovedProduct;
import org.openlmis.migration.tool.openlmis.referencedata.domain.Orderable;
import org.openlmis.migration.tool.openlmis.referencedata.domain.OrderableDisplayCategory;
import org.openlmis.migration.tool.openlmis.referencedata.domain.Program;
import org.openlmis.migration.tool.openlmis.referencedata.domain.ProgramOrderable;
import org.openlmis.migration.tool.openlmis.referencedata.repository.custom.OlmisFacilityTypeApprovedProductRepositoryCustom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.Collection;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

public class OlmisFacilityTypeApprovedProductRepositoryImpl
    implements OlmisFacilityTypeApprovedProductRepositoryCustom {

  @Autowired
  @Qualifier("olmisEntityManager")
  private EntityManager entityManager;

  @Override
  public Collection<FacilityTypeApprovedProduct> searchProducts(UUID facilityId, UUID programId,
                                                                boolean fullSupply) {
    checkNotNull(facilityId);

    CriteriaBuilder builder = entityManager.getCriteriaBuilder();

    CriteriaQuery<FacilityTypeApprovedProduct> query = builder.createQuery(
        FacilityTypeApprovedProduct.class
    );

    Root<FacilityTypeApprovedProduct> ftap = query.from(FacilityTypeApprovedProduct.class);
    Root<Facility> facility = query.from(Facility.class);

    Join<Facility, FacilityType> fft = facility.join("type");

    Join<FacilityTypeApprovedProduct, FacilityType> ft = ftap.join("facilityType");
    Join<FacilityTypeApprovedProduct, ProgramOrderable> pp = ftap.join("programOrderable");

    Join<ProgramOrderable, Program> program = pp.join("program");

    Predicate conjunction = builder.conjunction();
    if (programId != null) {
      conjunction = builder.and(conjunction, builder.equal(program.get("id"), programId));
    }
    conjunction = builder.and(conjunction, builder.equal(fft.get("id"), ft.get("id")));
    conjunction = builder.and(conjunction, builder.equal(facility.get("id"), facilityId));
    conjunction = builder.and(conjunction, builder.equal(pp.get("fullSupply"), fullSupply));
    conjunction = builder.and(conjunction, builder.isTrue(pp.get("active")));

    query.select(ftap);
    query.where(conjunction);

    Join<ProgramOrderable, OrderableDisplayCategory> category = pp.join("orderableDisplayCategory");
    Join<ProgramOrderable, Orderable> orderable = pp.join("product");

    query.orderBy(
        builder.asc(category.get("orderedDisplayValue").get("displayOrder")),
        builder.asc(category.get("orderedDisplayValue").get("displayName")),
        builder.asc(orderable.get("productCode"))
    );

    return entityManager.createQuery(query).getResultList();
  }

}
