# SkillSetter

SkillSetter is a Java-based application that helps users find compatible teammates based on their skills, goals, and availability.

The idea is simple — instead of randomly forming teams for hackathons, projects, or study groups, SkillSetter matches people intelligently so teams are balanced and productive.

---

## 🚀 Features

- User Registration (dynamic input)
- Skill-based Matching System
- Compatibility Score (%)
- Match Ranking (best matches first)
- “Why Match” explanation
- Join / Build team modes
- Connection Request System
- Contact sharing after acceptance
- Menu-driven console interface
- Basic UI (Swing - optional/extendable)

---

## 🧠 How It Works

1. Users enter their details:
   - Skills (frontend, backend, ML, etc.)
   - Skill level
   - Availability
   - Goal (Hackathon, PBL, Startup, Study)
   - Role preference

2. The system compares users based on:
   - Complementary skills
   - Similar goals
   - Availability match

3. A compatibility score is generated.

4. Users can:
   - View ranked matches
   - Send connection requests
   - Accept/reject requests

5. Contact details are shared only after acceptance.

---

## 🛠️ Tech Stack

- Language: Java
- Concepts: OOP, Collections (ArrayList, HashMap)
- UI: Java Swing (basic)
- No database (in-memory storage)

---

## 📂 Project Structure

SkillSetter/
│── User.java
│── MatchEngine.java
│── ConnectionRequest.java
│── SkillSetterApp.java
│── SkillSetterUI.java (optional)
│── Main.java

---

## ▶️ How to Run

### 1. Clone the repository

```bash
1. Clone repo
git clone https://github.com/arch5d/SkillSetter.git
cd SkillSetter
2. Compile all Java files
javac *.java
3. Run the application
java Main
```

 🧪 Example Flow
1. Register User
2. View Matches
3. Send Request
4. Respond to Requests
5. Exit

📌 Use Cases
Hackathon team formation
College PBL groups
Startup team building
Study partner matching

---

## 👩‍💻 Author

Built as a project to solve real team formation problems in student and developer communities.
