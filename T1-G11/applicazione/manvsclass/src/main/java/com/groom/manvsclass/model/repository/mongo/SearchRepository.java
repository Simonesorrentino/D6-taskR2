package com.groom.manvsclass.model.repository.mongo;

import com.groom.manvsclass.model.ClassUT;

import java.util.List;

public interface SearchRepository {

    List<ClassUT> findByText(String text);
}
