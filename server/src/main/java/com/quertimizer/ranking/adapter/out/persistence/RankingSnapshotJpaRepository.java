package com.quertimizer.ranking.adapter.out.persistence;

import com.quertimizer.judge.domain.model.DbmsType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RankingSnapshotJpaRepository extends JpaRepository<RankingSnapshotJpaEntity, RankingSnapshotJpaId> {

    List<RankingSnapshotJpaEntity> findAllBySnapshotIdAndDbmsType(String snapshotId, DbmsType dbmsType);

    @Modifying
    @Query("""
            delete from RankingSnapshotJpaEntity snapshot
            where snapshot.snapshotId <> :activeSnapshotId
            """)
    void deleteInactiveSnapshots(@Param("activeSnapshotId") String activeSnapshotId);
}
