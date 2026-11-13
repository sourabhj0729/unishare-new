# UniShare - College Resource Sharing Platform

A Java Swing desktop application backed by MySQL using JDBC.

## Current features
- Student registration and login with salted PBKDF2 password hashing.
- Add, edit, delete, browse, search and filter shared resources.
- Borrow and return resources.
- Seven-day borrowing period with a due date displayed in the resource table.
- Borrowing history for both items borrowed by you and items lent by you.
- Community Hall of Fame with live rankings for top resource contributors, top lenders and reliable on-time borrowers.
- Digital Library for uploading, browsing, searching, filtering and downloading academic PDFs, presentations, documents and notes.
- Community titles: Rising Star (1+ activities), Expert (5+), Master (10+) and Grandmaster (20+).
- History records include borrowed date, due date, returned date and current status (Borrowed, Overdue or Returned).

## Requirements
- JDK 11 or newer.
- MySQL Server 8.x running locally.
- MySQL Connector/J JAR in the `lib/` folder.

## Setup and run
1. Start MySQL Server.
2. Copy `db.properties.example` to `db.properties`.
3. Edit `db.properties` and enter your local MySQL username and password.
4. On Windows, run `run.bat`. On Linux/macOS, run `./run.sh`.
5. Create an account and sign in.

The application creates the `unishare` database and required tables automatically. If you already have an older UniShare database, startup adds the `resources.due_date` column and creates `borrowing_history` without deleting existing users or resources.

## Digital Library

- Students can browse and download existing documents without uploading anything.
- Uploading is optional and supports PDF, PPT, PPTX, DOC, DOCX and TXT files up to 25 MB.
- Documents are categorized by subject, department and semester, with optional faculty and description fields.
- File metadata is stored in MySQL; uploaded files are stored under `uploads/documents/` and are excluded from Git.
- The Open action launches the file in the computer's default viewer. Embedded PDF rendering is not included yet.
- A shared library across multiple computers requires a shared MySQL server and shared file storage; the current local setup stores files on the machine running the application.


## AI Study Materials

The Digital Library includes an optional AI study assistant for a selected document. It can generate:
- Multiple-choice questions with answers and explanations.
- Flashcards for active recall.
- Important exam-oriented questions.
- Short- and long-answer questions with model answers.
- Chapter-wise revision notes.

### AI service setup (Windows)

1. Install Python 3.10 or newer.
2. Set an OpenAI API key in the same Command Prompt session: `set OPENAI_API_KEY=your_api_key`.
3. Run `run_ai_service.bat` from the project root. The script creates a virtual environment, installs the Python dependencies and starts the local service on `127.0.0.1:8000`.
4. Keep that terminal open while using UniShare.
5. Start UniShare, open Digital Library, select a document and click **AI Study Materials**.

You can optionally set `OPENAI_MODEL` to select a model supported by your API account; the default is `gpt-4.1-mini`. API usage may incur charges. Never commit your API key to Git.

### Extraction support and limitations

- Text extraction currently supports PDF, DOCX, PPTX and TXT.
- Legacy DOC/PPT files must be converted to DOCX/PPTX first.
- Scanned/image-only PDFs need OCR before they can be processed; OCR is not included in this first version.
- The AI service processes up to 45,000 extracted characters per document request.
- Generated content is a study aid, not a substitute for checking the original document. The service is local to the computer running UniShare; it is not a shared campus AI server.

## Borrowing rules in this version
- A resource can be borrowed only when it is available.
- Students cannot borrow their own resources.
- The default borrowing period is seven calendar days.
- Every borrow creates a separate history row. Returning an item closes the active history row, so previous borrowing cycles remain visible.
- An item returned after its due date is shown as `Returned` in history; the returned and due dates are both retained so late-return/fine calculation can be added next.

## Hall of Fame

The Hall of Fame ranks students by verified sharing and borrowing activity. Scores are calculated from the existing `resources` and `borrowing_history` tables; no manual score editing or extra database table is required.

### Contribution points

- +10 points for each currently listed resource.
- +20 points for each completed lending (a resource returned to its owner).
- +15 points for each on-time return.
- +5 points for each late return (responsible completion still receives credit, but fewer points).
- +5 bonus points for each currently listed book or study-notes resource.

The leaderboard uses total points, then successful lending, on-time returns, resource count and student name as tie-breakers. The top seven positions receive, in order: Campus Legend, Grandmaster, Master, Elite Contributor, Rising Star, Community Hero and Knowledge Guardian. Other students appear as Contributors.

Scores are derived from current database records whenever the Hall of Fame is opened. Deleting a currently listed resource removes its listing points; completed borrowing-history records remain available for lending and return points.

## Manual compile (optional)

```bash
javac -encoding UTF-8 -cp "lib/*" -d out src/unishare/*.java
java -cp "out;lib/*" unishare.Main
```

On Linux/macOS, use `:` instead of `;` in the Java classpath.
