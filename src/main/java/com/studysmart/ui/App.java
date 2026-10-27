package com.studysmart.ui;

import com.studysmart.db.Database;
import com.studysmart.db.MaterialDAO;
import com.studysmart.db.QuizDAO;
import com.studysmart.db.ResponseDAO;
import com.studysmart.db.StudyPlanDAO;
import com.studysmart.db.UserDAO;
import com.studysmart.model.Quiz;
import com.studysmart.model.Response;
import com.studysmart.model.User;
import com.studysmart.service.AnswerEvaluator;
import com.studysmart.service.AuthService;
import com.studysmart.service.OpenAIClient;
import com.studysmart.service.PdfService;
import com.studysmart.service.QuizGenerator;
import com.studysmart.service.StudyPlanGenerator;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class App extends Application {
    private Stage stage;
    private User currentUser;

    private AuthService authService;
    private PdfService pdfService;
    private QuizGenerator quizGenerator;
    private AnswerEvaluator answerEvaluator;
    private StudyPlanGenerator studyPlanGenerator;
    private MaterialDAO materialDAO;
    private QuizDAO quizDAO;
    private ResponseDAO responseDAO;
    private StudyPlanDAO studyPlanDAO;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        try {
            Database db = new Database("studysmart.db");
            OpenAIClient ai = new OpenAIClient();
            materialDAO = new MaterialDAO(db);
            quizDAO = new QuizDAO(db);
            responseDAO = new ResponseDAO(db);
            studyPlanDAO = new StudyPlanDAO(db);
            authService = new AuthService(new UserDAO(db));
            pdfService = new PdfService();
            quizGenerator = new QuizGenerator(ai, quizDAO);
            answerEvaluator = new AnswerEvaluator(quizDAO, responseDAO);
            studyPlanGenerator = new StudyPlanGenerator(ai, studyPlanDAO);
        } catch (SQLException e) {
            showError("Could not open the database: " + e.getMessage());
            Platform.exit();
            return;
        }

        stage.setTitle("Study Smart AI");
        showLogin();
        stage.show();
    }

    public void showLogin() {
        currentUser = null;
        setScreen(new LoginScreen(this).getView());
    }

    public void showDashboard() {
        setScreen(new DashboardScreen(this).getView());
    }

    public void showQuiz(Quiz quiz) {
        setScreen(new QuizScreen(this, quiz).getView());
    }

    public void showResults(Quiz quiz, List<Response> responses, String studyPlan) {
        setScreen(new ResultsScreen(this, quiz, responses, studyPlan).getView());
    }

    private void setScreen(Parent root) {
        Scene scene = new Scene(root, 820, 680);
        scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());
        stage.setScene(scene);
    }

    // Runs slow work (PDF reading, OpenAI calls) off the UI thread so the window does not freeze
    public <T> void runTask(Callable<T> work, Consumer<T> onSuccess, Consumer<String> onError) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return work.call();
            }
        };
        task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
        task.setOnFailed(e -> onError.accept(errorMessage(task.getException())));
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    public static String errorMessage(Throwable e) {
        if (e instanceof SQLException) {
            return "Database error: " + e.getMessage();
        }
        if (e.getMessage() == null) {
            return "Something went wrong: " + e.getClass().getSimpleName();
        }
        return e.getMessage();
    }

    public static void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setHeaderText(null);
        alert.setTitle("Study Smart AI");
        alert.showAndWait();
    }

    public User getCurrentUser() { return currentUser; }
    public void setCurrentUser(User user) { this.currentUser = user; }
    public AuthService getAuthService() { return authService; }
    public PdfService getPdfService() { return pdfService; }
    public QuizGenerator getQuizGenerator() { return quizGenerator; }
    public AnswerEvaluator getAnswerEvaluator() { return answerEvaluator; }
    public StudyPlanGenerator getStudyPlanGenerator() { return studyPlanGenerator; }
    public MaterialDAO getMaterialDAO() { return materialDAO; }
    public QuizDAO getQuizDAO() { return quizDAO; }
    public ResponseDAO getResponseDAO() { return responseDAO; }
    public StudyPlanDAO getStudyPlanDAO() { return studyPlanDAO; }

    public static void main(String[] args) {
        launch(args);
    }
}
