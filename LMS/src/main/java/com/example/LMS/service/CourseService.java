package com.example.LMS.service;


import com.example.LMS.model.Course;
import com.example.LMS.model.Quiz;
import com.example.LMS.repository.CourseRepository;
import com.example.LMS.repository.LessonRepository;
import com.example.LMS.repository.QuizRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CourseService {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private LessonRepository lessonRepository;


    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    public Course getCourseById(Long id) {
        return courseRepository.findById(id).orElse(null);
    }

    public void addCourse(Course course) {
        courseRepository.save(course);
    }

    public void deleteCourse(Long id) {
        courseRepository.deleteById(id);
    }

    public void updateCourse(Course course) {
        courseRepository.save(course);
    }

    // Tìm kiếm khóa học theo tên
    public List<Course> searchCoursesByName(String name) {
        return courseRepository.findByNameContainingIgnoreCase(name);
    }

    public List<Course> getCoursesByCatalog(Long catalogId) {
        return courseRepository.findByCourseCatalog_Id(catalogId);
    }

    public Long getCourseIdByQuizId(Long quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz không tồn tại với ID: " + quizId));

        System.out.println("Quiz ID: " + quizId + ", Course ID: " + quiz.getCourse().getId());
        return quiz.getCourse().getId();
    }


}
