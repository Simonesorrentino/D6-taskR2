package com.groom.manvsclass.model.repository;

import java.util.List;

public interface SearchRepository {

    List<ClassUT> findByText(String text);
}
