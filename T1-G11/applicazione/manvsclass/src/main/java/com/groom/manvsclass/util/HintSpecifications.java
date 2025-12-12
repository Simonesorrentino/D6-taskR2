package com.groom.manvsclass.util;

import com.groom.manvsclass.model.Hint;
import com.groom.manvsclass.model.enums.HintTypeEnum;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;

import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HintSpecifications {

    public static Specification<Hint> withDynamicQuery(Map<String, String> queryParams) {

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (CollectionUtils.isEmpty(queryParams)) {
                //Lista predicates rimane vuota e recupero tutti gli hints
            } else {

                String typeParam = queryParams.get("type");
                if (StringUtils.hasText(typeParam)) {
                    try {
                        HintTypeEnum type = HintTypeEnum.valueOf(typeParam.toUpperCase());
                        predicates.add(criteriaBuilder.equal(root.get("type"), type));
                    } catch (IllegalArgumentException e) {
                        throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Tipo non valido");
                    }
                }

                String classUtNameParam = queryParams.get("classUTName");
                if (StringUtils.hasText(classUtNameParam)) {
                    predicates.add(criteriaBuilder.equal(root.get("classUt").get("name"), classUtNameParam));
                }

                String orderParam = queryParams.get("order");
                if (StringUtils.hasText(orderParam)) {
                    try {
                        Integer order = Integer.parseInt(orderParam);
                        predicates.add(criteriaBuilder.equal(root.get("order"), order));
                    } catch (NumberFormatException e) {
                        throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Ordine non valido");
                    }
                }
            }

            query.orderBy(criteriaBuilder.asc(root.get("order")));

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
