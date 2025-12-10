package com.groom.manvsclass.controller;

import com.groom.manvsclass.model.dto.HintResponse;
import com.groom.manvsclass.service.HintService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping("/hints")
public class HintController {

    @Autowired
    private HintService hintService;

    @GetMapping()
    public ResponseEntity<List<HintResponse>> getHints(
            @CookieValue(name = "jwt") String jwtToken,
            @RequestParam() Map<String, String> queryParams) {
        List<HintResponse> response = hintService.getHints(queryParams, jwtToken);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping()
    public ResponseEntity<String> createHints(
            @CookieValue(name = "jwt") String jwtToken,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "images", required = false) List<MultipartFile> images) {
        String response = hintService.createHintsFromFile(file, images, jwtToken);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
