package com.example.LMS.service;

import com.example.LMS.model.CourseCatalog;
import com.example.LMS.repository.CourseCatalogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CourseCatalogService {

    @Autowired
    private CourseCatalogRepository courseCatalogRepository;

    public List<CourseCatalog> getAllCourseCatalog() {
        return courseCatalogRepository.findAll();
    }

    public CourseCatalog getCourseCatalogById(Long id) {
        return courseCatalogRepository.findById(id).orElse(null);
    }

    public CourseCatalog saveCourseCatalog(CourseCatalog courseCatalog) {
        return courseCatalogRepository.save(courseCatalog);
    }

    public void deleteCourseCatalog(Long id) {
        courseCatalogRepository.deleteById(id);
    }
}
