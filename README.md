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
- Menu-driven console interface
- Basic UI (Swing - optional/extendable)
- Data persistence (users saved to file)
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
- UI: Java Swing (basic)
- Data Storage: Simple text file persistence
- No external dependencies

---

## 📂 Project Structure

SkillSetter/
│── Skill.java (skill with level representation)
│── SkillManager.java (manages available skills)
│── User.java (user model)
│── MatchEngine.java (matching algorithm)
│── ConnectionRequest.java (request system)
│── DataManager.java (file persistence)
│── SkillSetterApp.java (main application)
│── SkillSetterUI.java (Swing UI)
│── users.txt (data file - created automatically)

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
javac -cp src src/*.java
```

### 3. Run the application

**Console Version:**
```bash
java -cp src SkillSetterApp
```

**GUI Version:**
```bash
java -cp src SkillSetterApp ui
```

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

- User data is automatically saved to `users.txt`
- Data persists between application runs
- No database required - simple file-based storage

---

## 👩‍💻 Author

Built as a project to solve real team formation problems in student and developer communities.
[PBL  JAVA-IV-T119  SYNAPSE]