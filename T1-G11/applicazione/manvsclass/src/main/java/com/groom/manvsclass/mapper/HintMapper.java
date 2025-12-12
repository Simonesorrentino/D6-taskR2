package com.groom.manvsclass.mapper;

import com.groom.manvsclass.model.ClassUT;
import com.groom.manvsclass.model.Hint;
import com.groom.manvsclass.model.dto.HintResponse;
import com.groom.manvsclass.model.repository.AdminRepository;
import com.groom.manvsclass.model.repository.ClassUTRepository;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Mapper(componentModel = "spring")
public abstract class HintMapper {

    protected AdminRepository adminRepository;
    protected ClassUTRepository classUTRepository;

    @Autowired
    public void setAdminRepository(AdminRepository adminRepository) {
        this.adminRepository = adminRepository;
    }

    @Autowired
    public void setClassUTRepository(ClassUTRepository classUTRepository) {
        this.classUTRepository = classUTRepository;
    }

    @Mappings({
            @Mapping(source = "classUTName", target = "classUt", qualifiedByName = "mapClassUTFromName"),
            @Mapping(source = "imageUri", target = "imageUri")
    })
    public abstract Hint dtoToEntity(com.groom.manvsclass.model.dto.Hint hint);

    @Mappings({
            @Mapping(source = "classUt.name", target = "classUTName")
    })
    public abstract com.groom.manvsclass.model.dto.Hint entityToDto(Hint hint);

    @Mappings({
            @Mapping(source = "admin.email", target = "adminEmail"),
            @Mapping(source = "classUt.name", target = "classUTName")
    })
    public abstract HintResponse toResponse(Hint hint);

    public abstract List<com.groom.manvsclass.model.dto.Hint> mapList(List<Hint> hintList);

    public abstract List<HintResponse> toResponseList(List<Hint> hintList);

    @Named("mapClassUTFromName")
    public ClassUT mapClassUTFromName(String classUTName) {
        if (classUTName == null) {
            return null;
        }
        return classUTRepository.findById(classUTName).orElse(null);
    }

}