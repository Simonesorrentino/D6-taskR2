package com.groom.manvsclass.service;

import com.groom.manvsclass.model.Hint;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface HintService {

    ResponseEntity<List<Hint>> getHints(Map<String, String> queryParams, String jwtToken);

    ResponseEntity<String> createHintsFromFile(MultipartFile file, String jwtToken);

    ResponseEntity<String> deleteHintByClassUT(String classUT, String jwtToken);

    ResponseEntity<String> deleteHintByClassUTAndOrder(String classUT, Integer order, String jwtToken);

}
