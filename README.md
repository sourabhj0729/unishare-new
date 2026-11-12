# UniShare - College Resource Sharing Platform

A Java Swing desktop application backed by MySQL using JDBC.

## Current features
- Student registration and login with salted PBKDF2 password hashing.
- Add, edit, delete, browse, search and filter shared resources.
- Borrow and return resources.
- Seven-day borrowing period with a due date displayed in the resource table.
- Borrowing history for both items borrowed by you and items lent by you.
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
