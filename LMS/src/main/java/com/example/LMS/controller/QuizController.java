package com.example.LMS.controller;

import com.example.LMS.model.Course;
import com.example.LMS.model.Lesson;
import com.example.LMS.model.Quiz;
import com.example.LMS.model.QuizQuestion;
import com.example.LMS.repository.LessonRepository;
import com.example.LMS.service.CourseService;
import com.example.LMS.service.LessonService;
import com.example.LMS.service.QuizQuestionService;
import com.example.LMS.service.QuizService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/quizzes")
public class QuizController {

    @Autowired
    private QuizService quizService;

    @Autowired
    private CourseService courseService;

    @Autowired
    private LessonService lessonService;

    @Autowired
    private QuizQuestionService quizQuestionService;

    @Autowired
    private LessonRepository lessonRepository;

    @GetMapping("/list/{courseId}/{lessonId}")
    public String listQuizzes(@PathVariable Long courseId,@PathVariable("lessonId") Long lessonId, Model model) {
        List<Quiz> quizzes = quizService.getQuizzesByLesson(lessonId);
        model.addAttribute("quizzes", quizzes);
        model.addAttribute("courseId", courseId);

        // Đếm số lượng câu hỏi cho từng quiz
        Map<Long, Integer> questionCounts = new HashMap<>();
        for (Quiz quiz : quizzes) {
            int count = quizQuestionService.countQuestionsByQuizId(quiz.getId());
            questionCounts.put(quiz.getId(), count);
        }
        model.addAttribute("questionCounts", questionCounts);

        return "quiz/list"; // Trả về template Thymeleaf
    }


    @GetMapping("/add/{courseId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public String addQuizForm(@PathVariable Long courseId, Model model) {
        List<Lesson> lessons = lessonRepository.findByCourseId(courseId);
        model.addAttribute("lessons", lessons);
        model.addAttribute("quiz", new Quiz());
        model.addAttribute("courseId", courseId);
        return "quiz/add";
    }

    @PostMapping("/add")
    @PreAuthorize("hasAuthority('ADMIN')")
    public String saveQuiz(@RequestParam("courseId") Long courseId,@RequestParam("lessonId") Long lessonId, @ModelAttribute Quiz quiz, RedirectAttributes redirectAttributes) {
        if (courseId == null) {
            redirectAttributes.addFlashAttribute("error", "Course ID is missing or invalid.");
            return "redirect:/courses"; // Redirect về danh sách khóa học
        }
        Course course = courseService.getCourseById(courseId);
        if (course == null) {
            redirectAttributes.addFlashAttribute("error", "Course not found.");
            return "redirect:/courses";
        }
        Lesson lesson = lessonService.getLessonById(lessonId);
        if (lesson == null) {
            redirectAttributes.addFlashAttribute("error", "Lesson not found.");
            return "redirect:/lessons";
        }
        quiz.setCourse(course);
        quiz.setLesson(lesson);
        // Thiết lập thời gian tạo bài trắc nghiệm là thời gian hiện tại
        quiz.setCreationTime(LocalDateTime.now());
        quizService.saveQuiz(quiz);
        return "redirect:/quizzes/list/" + courseId + "/" + lessonId;
    }


    @GetMapping("/edit/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public String editQuizForm(@PathVariable Long id, Model model) {
        Quiz quiz = quizService.getQuizById(id);
        model.addAttribute("quiz", quiz);

        // Lấy courseId của quiz hiện tại
        Long courseId = quiz.getCourse().getId();
        model.addAttribute("courseId", courseId);

        // Lấy danh sách các bài học thuộc khóa học này
        List<Lesson> lessons = lessonService.getLessonsByCourseId(courseId);
        model.addAttribute("lessons", lessons);

        return "quiz/edit"; // Trả về trang chỉnh sửa quiz
    }

    @PostMapping("/edit/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public String updateQuiz(@PathVariable Long id,
                             @ModelAttribute("quiz") Quiz quiz,
                             @RequestParam("courseId") Long courseId,
                             @RequestParam("lessonId") Long lessonId,
                             RedirectAttributes redirectAttributes) {

        // Kiểm tra xem bài quiz có tồn tại không
        Quiz existingQuiz = quizService.getQuizById(id);
        if (existingQuiz == null) {
            redirectAttributes.addFlashAttribute("error", "Quiz not found.");
            return "redirect:/courses"; // Redirect nếu không tìm thấy quiz
        }

        // Cập nhật các thông tin quiz
        existingQuiz.setTitle(quiz.getTitle());
        existingQuiz.setDescription(quiz.getDescription());
        existingQuiz.setDuration(quiz.getDuration());

        // Cập nhật bài học liên kết với quiz
        Lesson lesson = lessonService.getLessonById(lessonId);
        if (lesson != null) {
            existingQuiz.setLesson(lesson); // Gán lesson cho quiz
        } else {
            redirectAttributes.addFlashAttribute("error", "Lesson not found.");
            return "redirect:/lessons"; // Redirect nếu không tìm thấy lesson
        }

        // Lưu lại quiz đã chỉnh sửa
        quizService.saveQuiz(existingQuiz);

        // Chuyển hướng đến trang danh sách bài học của khóa học
        return "redirect:/quizzes/list/" + courseId + "/" + lessonId;
    }

    @GetMapping("/delete/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public String deleteQuiz(@PathVariable Long id) {
        // Lấy thông tin bài quiz
        Quiz quiz = quizService.getQuizById(id);
        if (quiz == null) {
            throw new IllegalArgumentException("Không tìm thấy bài quiz với ID: " + id);
        }
        // Lấy courseId và lessonId từ quiz
        Long courseId = quiz.getCourse().getId();
        Long lessonId = quiz.getLesson().getId(); // Giả sử Quiz có liên kết với Lesson
        // Xóa bài quiz
        quizService.deleteQuiz(id);
        // Chuyển hướng về danh sách quiz với courseId và lessonId
        return "redirect:/quizzes/list/" + courseId + "/" + lessonId;
    }
}