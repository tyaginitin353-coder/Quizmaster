# QuizCraft — Java-Based Online Quiz Platform (Project 37)

A Review 1 web prototype based on Project Topic 37 from the supplied topic list. The Java standard-library HTTP server serves a responsive HTML/CSS/JavaScript interface and exposes REST-style endpoints. Data is persisted locally using Java object serialization in `data/quizcraft-store.bin`.

## Requirements
- JDK 17 or newer (the code uses the Java standard library only)
- A modern browser
- Port 8080 available

## Run on Windows
1. Extract the ZIP folder.
2. Install a JDK if `javac -version` does not work in Command Prompt.
3. Double-click `run.bat` or open Command Prompt in this folder and run `run.bat`.
4. Open `http://localhost:8090` in your browser.
5. Stop the server with Ctrl+C in the console.

## Run on macOS/Linux
Open Terminal in this folder and run `./run.sh`, then open `http://localhost:8090`.

## Demo workflows
- Participant: take an approved timed quiz, see score/explanation, review history, leaderboard, messages and reminders.
- Quiz Creator: submit an MCQ quiz for approval, view quiz library and attempts.
- Admin: create/deactivate users, approve/reject/delete quizzes, view platform reports, update settings, review alerts.
- Refresh the browser to verify that changes remain saved in the local data file.

## Demo data
The app starts with sample Admin, Quiz Creator and Participant records; two approved quizzes and one pending quiz. The role selector is a **workflow preview only**, not authentication. Do not enter real personal information or expose this server to the internet.

## Review 1 scope and known limitations
This is an educational prototype, not a production deployment. It demonstrates the principal workflows in Topic 37. Current quiz creation supports one MCQ per submitted quiz. Role switching is not secured; passwords, real authentication/authorization, multi-user network deployment, database integration, email/push notifications, advanced analytics, and hardened input/security controls are not implemented. Data is stored on the server machine in a serialized local file. The platform should be run locally for demonstration unless the instructor approves a deployment setup.

## Suggested demonstration order
1. Open Overview as Participant; start Java Fundamentals and submit an answer.
2. Open My Results and Leaderboard.
3. Switch role to Quiz Creator; create a quiz and submit it for approval.
4. Switch role to Admin; approve/reject the submitted quiz and add a demo user.
5. Open Performance Reports, export CSV and update settings.
6. Restart the server and verify persisted demo records.

## Review 1 report and presentation
See `docs/` for the project report (DOCX and PDF), review presentation, requirements/test plan, and system diagrams. Replace all team/member/faculty placeholders and insert real screenshots before submission. Record only tests you actually run.
