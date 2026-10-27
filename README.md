# Study Smart AI

A desktop app that turns a PDF of study notes into a 10-question true/false quiz using the OpenAI API, marks your answers and then writes a personalised study plan for your weak topics.

Built with Java 17, JavaFX, SQLite, PDFBox and the OpenAI API.

## Running it


You only need Java 17 or newer. The Maven wrapper downloads everything else the first time.

1. Set your OpenAI API key (PowerShell):
   ```powershell
   $env:OPENAI_API_KEY = "sk-..."
   # optional: $env:OPENAI_MODEL = "gpt-4o-mini"
   ```
2. Start the app:
   ```powershell
   .\mvnw.cmd javafx:run
   ```

### macOS (one command)

```bash
bash setup-mac.sh        # installs Homebrew + Java 17 if missing, asks for your API key, starts the app
bash setup-mac.sh test   # same setup, then runs the tests
```

Data is saved in `studysmart.db` in the project folder.

## Running the tests

```powershell
.\mvnw.cmd test
```

The tests use a fake OpenAI client, so they don't need an API key or internet access.

## Project structure

| Package | Contents |
|---|---|
| `model` | `User`, `StudyMaterial`, `Quiz`, `Question`, `Response`, `StudyPlan` |
| `db` | `Database` (creates the 6 tables) and one DAO per table group |
| `service` | `AuthService`, `PdfService`, `OpenAIClient`, `QuizGenerator`, `AnswerValidator`, `AnswerEvaluator`, `StudyPlanGenerator` |
| `ui` | `App`, `LoginScreen`, `DashboardScreen`, `QuizScreen`, `ResultsScreen` |
