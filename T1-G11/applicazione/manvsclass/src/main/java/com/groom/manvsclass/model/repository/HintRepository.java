package com.groom.manvsclass.model.repository;

import com.groom.manvsclass.model.Hint;
import com.groom.manvsclass.model.enums.HintTypeEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HintRepository extends JpaRepository<Hint, Long>, JpaSpecificationExecutor<Hint> {

    @Query("SELECT h FROM HintEntity h WHERE h.content = :content AND h.type = :type AND " +
            "((h.type = 'CLASS' AND h.classUt.name = :classUtName) OR (h.type = 'GENERIC' AND h.classUt IS NULL))")
    Optional<Hint> findUniqueHint(@Param("content") String content,
                                  @Param("type") HintTypeEnum type,
                                  @Param("classUtName") String classUtName);

    @Query("SELECT h FROM HintEntity h " +
            "WHERE h.type = :type AND " +
            "((h.type = 'CLASS' AND h.classUt.name = :classUtName) OR " +
            " (h.type = 'GENERIC' AND h.classUt IS NULL AND :classUtName IS NULL))" +
            "ORDER BY h.order DESC")
    Optional<Hint> findMaxOrderHint(
            @Param("classUtName") String classUtName,
            @Param("type") HintTypeEnum type
    );

    Hint findByContentAndTypeAndClassUtName(String content, HintTypeEnum type, String classUtName);

    List<Hint> findByClassUtName(String classUtName);

    Hint findByClassUtNameAndOrder(String classUtName, Integer order);

    List<Hint> findByType(HintTypeEnum type);

    Hint findByTypeAndOrder(HintTypeEnum type, Integer order);


}
