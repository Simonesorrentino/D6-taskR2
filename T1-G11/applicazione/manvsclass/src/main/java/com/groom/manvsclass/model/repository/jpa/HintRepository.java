package com.groom.manvsclass.model.repository.jpa;

import com.groom.manvsclass.model.entity.HintEntity;
import com.groom.manvsclass.model.enums.HintTypeEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HintRepository extends JpaRepository<HintEntity, Long>, JpaSpecificationExecutor<HintEntity> {

    @Query("SELECT h FROM HintEntity h WHERE h.content = :content AND h.type = :type AND " +
            "((h.type = 'CLASS' AND h.classUt.name = :classUtName) OR (h.type = 'GENERIC' AND h.classUt IS NULL))")
    Optional<HintEntity> findUniqueHint(@Param("content") String content,
                                        @Param("type") HintTypeEnum type,
                                        @Param("classUtName") String classUtName);

    @Query("SELECT h FROM HintEntity h " +
            "WHERE h.type = :type AND " +
            "((h.type = 'CLASS' AND h.classUt.name = :classUtName) OR " +
            " (h.type = 'GENERIC' AND h.classUt IS NULL AND :classUtName IS NULL))" +
            "ORDER BY h.order DESC")
    Optional<HintEntity> findMaxOrderHint(
            @Param("classUtName") String classUtName,
            @Param("type") HintTypeEnum type
    );

    HintEntity findByContentAndTypeAndClassUtName(String content,  HintTypeEnum type, String classUtName);

    List<HintEntity> findByClassUtName(String classUtName);

    HintEntity findByClassUtNameAndOrder(String classUtName, Integer order);

    List<HintEntity> findByType(HintTypeEnum type);

    HintEntity findByTypeAndOrder(HintTypeEnum type, Integer order);


}
