package io.levelops.api.controllers;

import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
@Log4j2
@SuppressWarnings("unused")
public class DefaultController {

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<Void> getSlash() {
        return ResponseEntity.ok().build();
    }

}
