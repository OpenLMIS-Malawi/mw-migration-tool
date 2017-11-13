package mw.gov.health.lmis.migration.tool.openlmis.requisition.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mw.gov.health.lmis.migration.tool.openlmis.requisition.domain.Requisition;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface RequisitionRepository extends JpaRepository<Requisition, UUID> {

  List<Requisition> findByFacilityIdAndProgramIdAndProcessingPeriodId(UUID facilityId,
                                                                      UUID programId,
                                                                      UUID processingPeriodId);

  boolean existsByFacilityIdAndProgramIdAndProcessingPeriodId(UUID facilityId,
                                                              UUID programId,
                                                              UUID processingPeriodId);

  @Query(value = "SELECT r.programId, r.processingPeriodId, count(rli)"
          + " FROM Requisition r"
          + " INNER JOIN r.requisitionLineItems rli"
          + " WHERE r.processingPeriodId in (:periodIds)"
          + " GROUP BY r.programId, r.processingPeriodId")
  List<Object[]> countByProgramAndPeriod(@Param("periodIds") Set<UUID> periodIds);

}
