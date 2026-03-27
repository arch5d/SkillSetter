# SkillSetter

SkillSetter is a Java-based application that helps users find compatible teammates based on their skills, goals, and availability.

The idea is simple — instead of randomly forming teams for hackathons, projects, or study groups, SkillSetter matches people intelligently so teams are balanced and productive.

---

## 🚀 Features

- User Registration (dynamic input with skill selection)
- Skill-based Matching System with level consideration
- Compatibility Score (%)
- Match Ranking (best matches first)
- "Why Match" explanation with detailed reasons
- Join / Build team modes with team size specification
- Connection Request System
- Contact sharing after acceptance
- Java REST backend with web frontend (HTML/CSS/JS)
- JDBC persistence with SQLite database
- Profile deletion support
- Predefined skills + custom skill addition

---

## 🧠 How It Works

1. Users register with their details:
   - Name and email
   - Skills with proficiency levels (from predefined list or custom)
   - Availability hours per week
   - Role (Leader/Teammate/Learner)
   - Goal (Hackathon, PBL, Startup, Study)
   - Mode (Join existing team / Build new team)
   - Team size (for leaders only)

2. The system matches users based on:
   - Complementary skills (what others bring that you don't)
   - Skill level compatibility
   - Similar time commitment
   - Leader-Join team compatibility
   - Same goals
   - Different roles for team balance

3. A compatibility score is generated with detailed explanations.

4. Users can:
   - View ranked matches
   - Send connection requests
   - Accept/reject requests

5. Contact details are shared only after acceptance.

---

## 🛠️ Tech Stack

- Language: Java
- Concepts: OOP, Collections, File I/O
- Backend: Java HTTP Server (`com.sun.net.httpserver`)
- Frontend: HTML, CSS, JavaScript
- Data Storage: JDBC + SQLite (`skillsetter.db`)

---

## 📂 Project Structure

SkillSetter/
│── src/
│   │── ApiServer.java (REST API server)
│   │── DatabaseManager.java (JDBC persistence layer)
│   │── Skill.java, SkillManager.java, User.java, MatchEngine.java
│   │── ConnectionRequest.java
│── frontend/
│   │── index.html
│   │── style.css
│   │── app.js
│── lib/
│   │── sqlite-jdbc.jar
│── skillsetter.db (created automatically on first run)

---

## ▶️ How to Run

### Prerequisites
- Java JDK 8 or higher installed

### 1. Clone the repository

```bash
git clone https://github.com/arch5d/SkillSetter.git
cd SkillSetter
```

### 2. Compile all Java files
```bash
javac -cp ".;src;lib/*" src/*.java src/org/slf4j/*.java
```

### 3. Run the backend API

Default port (8080):
```bash
java -cp "src;lib/sqlite-jdbc.jar" ApiServer
```

Custom port (example 8081):
```bash
java -cp "src;lib/sqlite-jdbc.jar" ApiServer 8081
```

### 4. Open the frontend

Open `frontend/index.html` in browser.

If backend is not on `8080`, open with `api` query param:
```text
frontend/index.html?api=http://localhost:8081
```

### Quick start on Windows

Run backend (default 8080):
```bash
run-backend.bat
```

Run backend on custom port:
```bash
run-backend.bat 8090
```

Open frontend:
```bash
open-frontend.bat
```

Open frontend targeting custom backend port:
```bash
open-frontend.bat 8090
```

### API Endpoints

- `POST /api/register` register or update profile
- `GET /api/user?email=...` fetch single user profile
- `GET /api/users` list all users
- `GET /api/matches?email=...` list ranked matches with complementary skills
- `POST /api/requests` send connection request
- `GET /api/requests?email=...` receiver inbox (accept/reject after login)
- `PUT /api/requests` update request status (`ACCEPTED` or `REJECTED`)
- `DELETE /api/deleteProfile?email=...` delete profile and related data

---

## 🎯 Example Usage

1. **Register Users:**
   - Choose from 40+ predefined skills or add custom ones
   - Specify skill levels (Beginner/Intermediate/Advanced/Expert)
   - Set availability, role, goal, and mode
   - Leaders specify desired team size

2. **View Matches:**
   - See compatibility scores with detailed explanations
   - Matches consider complementary skills, time commitment, and team dynamics

3. **Connect:**
   - Send requests to potential teammates
   - Accept/reject incoming requests
   - Share contact info upon mutual acceptance

---

## 🔧 Matching Algorithm

The matching system evaluates compatibility across multiple dimensions:

- **Skill Complementarity (25% weight):** Rewards teams where members bring different skills
- **Skill Level Compatibility (up to 15%):** Bonuses for same levels (collaboration) or adjacent levels (mentoring)
- **Time Commitment Match (up to 20%):** Similar availability hours
- **Role Compatibility (up to 25%):** Leader + Join team matches, team size consideration
- **Goal Alignment (15%):** Same project goals
- **Role Balance (10%):** Different roles for well-rounded teams

---

## 💾 Data Persistence

- Data is stored in `skillsetter.db` using JDBC (SQLite)
- User profile deletion cascades to skills and requests
- Request acceptance is receiver-owned and enforced on backend

---

## 👩‍💻 Author

Built as a project to solve real team formation problems in student and developer communities.
[PBL  JAVA-IV-T119  SYNAPSE]