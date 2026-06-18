package com.example.kipscoachingkharian.dashboard;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.kipscoachingkharian.R;
import com.example.kipscoachingkharian.auth.LoginSelectionActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StudentDashboardActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private TextView tvStudentName, tvStudentClass;
    private ImageView ivMenu;
    private EditText etSearch;

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private BottomNavigationView bottomNav;

    // Containers for Horizontal Scroll (Upcoming Schedule)
    private LinearLayout containerUpcomingClasses;
    private LinearLayout containerUpcomingQuizzes;
    private LinearLayout containerUpcomingAssignments;

    // Grid Cards
    private CardView cardSubjects, cardClassSchedule, cardStudyMaterial, cardQuizzes;
    private CardView cardAssignments, cardReports, cardFees, cardAnnouncements;

    private List<CardView> allCards;
    private List<String> allCardLabels;

    // Handler for refreshing join button status every minute
    private Handler refreshHandler = new Handler();
    private Runnable refreshRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);

        initViews();
        setupSearchData();
        fetchStudentDetails();
        loadUpcomingSchedule();
        setupListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Har 60 seconds baad schedule refresh karein taake Join button live ho
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                loadUpcomingSchedule();
                refreshHandler.postDelayed(this, 60 * 1000);
            }
        };
        refreshHandler.postDelayed(refreshRunnable, 60 * 1000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        refreshHandler.removeCallbacks(refreshRunnable);
    }

    private void initViews() {
        tvStudentName  = findViewById(R.id.tvStudentName);
        tvStudentClass = findViewById(R.id.tvStudentClass);
        ivMenu         = findViewById(R.id.ivMenu);
        etSearch       = findViewById(R.id.etSearch);
        drawerLayout   = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        bottomNav      = findViewById(R.id.bottomNav);

        containerUpcomingClasses     = findViewById(R.id.containerUpcomingClasses);
        containerUpcomingQuizzes     = findViewById(R.id.containerUpcomingQuizzes);
        containerUpcomingAssignments = findViewById(R.id.containerUpcomingAssignments);

        cardSubjects      = findViewById(R.id.cardSubjects);
        cardClassSchedule = findViewById(R.id.cardClassSchedule);
        cardStudyMaterial = findViewById(R.id.cardStudyMaterial);
        cardQuizzes       = findViewById(R.id.cardQuizzes);
        cardAssignments   = findViewById(R.id.cardAssignments);
        cardReports       = findViewById(R.id.cardReports);
        cardFees          = findViewById(R.id.cardFees);
        cardAnnouncements = findViewById(R.id.cardAnnouncements);
    }

    private void setupSearchData() {
        allCards = new ArrayList<>();
        allCards.add(cardSubjects);
        allCards.add(cardClassSchedule);
        allCards.add(cardStudyMaterial);
        allCards.add(cardQuizzes);
        allCards.add(cardAssignments);
        allCards.add(cardReports);
        allCards.add(cardFees);
        allCards.add(cardAnnouncements);

        allCardLabels = new ArrayList<>();
        allCardLabels.add("Subjects");
        allCardLabels.add("Class Schedule");
        allCardLabels.add("Study Material");
        allCardLabels.add("Quizzes");
        allCardLabels.add("Assignments");
        allCardLabels.add("Progress Report");
        allCardLabels.add("Fee Status");
        allCardLabels.add("Announcements");
    }

    private void setupListeners() {
        navigationView.setNavigationItemSelectedListener(this);
        ivMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterCards(s.toString().trim());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_bottom_home) {
                return true;
            } else if (id == R.id.nav_bottom_feedback) {
                startActivity(new Intent(this, FeedbackActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_bottom_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                finish();
                return true;
            }
            return false;
        });

        cardSubjects.setOnClickListener(v -> startActivity(new Intent(this, SubjectsActivity.class)));
        cardClassSchedule.setOnClickListener(v -> startActivity(new Intent(this, ClassScheduleActivity.class)));
        cardStudyMaterial.setOnClickListener(v -> startActivity(new Intent(this, StudyMaterialActivity.class)));
        cardQuizzes.setOnClickListener(v -> startActivity(new Intent(this, QuizActivity.class)));
        cardAssignments.setOnClickListener(v -> startActivity(new Intent(this, AssignmentActivity.class)));
        cardReports.setOnClickListener(v -> startActivity(new Intent(this, ProgressActivity.class)));
        cardFees.setOnClickListener(v -> startActivity(new Intent(this, FeeActivity.class)));
        cardAnnouncements.setOnClickListener(v -> startActivity(new Intent(this, AnnouncementsActivity.class)));
    }

    // --- FIREBASE DATA ---

    private void fetchStudentDetails() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseDatabase.getInstance().getReference("Users").child(uid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String name = snapshot.child("name").getValue(String.class);
                            String cls = snapshot.child("className").getValue(String.class);
                            tvStudentName.setText(name != null ? name : "Student");
                            tvStudentClass.setText("Grade: " + (cls != null ? cls : "-"));
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void loadUpcomingSchedule() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseDatabase.getInstance().getReference("Users").child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot userSnap) {
                        String baseClass = userSnap.child("className").getValue(String.class);
                        String section   = userSnap.child("assignedSection").getValue(String.class);
                        if (baseClass == null) return;

                        // Teacher ki grade "9A" jaisi hoti hai (number + section).
                        // Kuch students ka className already poora grade hota hai
                        // (e.g. "9A" — section khud shamil), kuch ka sirf number
                        // ("9") aur section alag se "assignedSection" mein hota hai.
                        // Isliye sirf tab section append karo jab className sirf
                        // number ho (warna duplicate ho kar "9AA" jaisa galat
                        // result banega).
                        String trimmedBase = baseClass.trim();
                        boolean baseIsNumberOnly = trimmedBase.matches("\\d+");
                        String studentClass = baseIsNumberOnly && section != null && !section.trim().isEmpty()
                                ? trimmedBase + section.trim()
                                : trimmedBase;

                        // Student ne jo subjects enroll ki hain, unki list nikalo
                        final List<String> enrolledSubjects = new ArrayList<>();
                        for (DataSnapshot subSnap : userSnap.child("enrolledSubjects").getChildren()) {
                            String sub = subSnap.getValue(String.class);
                            if (sub != null) enrolledSubjects.add(sub);
                        }

                        // Step 1: upcomingSchedule node se data lo (quiz, assignment, class time)
                        FirebaseDatabase.getInstance().getReference("upcomingSchedule")
                                .addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot scheduleData) {
                                        if (containerUpcomingClasses == null) return;

                                        containerUpcomingClasses.removeAllViews();
                                        containerUpcomingQuizzes.removeAllViews();
                                        containerUpcomingAssignments.removeAllViews();

                                        // Sab matching items collect karo, phir har category se sirf
                                        // sab se nazdeek (earliest date/time) wala card dikhao.
                                        // (Class ke liye ab "Classes" node directly use hota hai —
                                        // loadLiveClassCard() — is liye yahan collect nahi karte.)
                                        List<DataSnapshot> quizItems       = new ArrayList<>();
                                        List<DataSnapshot> assignmentItems = new ArrayList<>();

                                        for (DataSnapshot snap : scheduleData.getChildren()) {
                                            String type   = snap.child("type").getValue(String.class);
                                            String target = snap.child("className").getValue(String.class);
                                            if (target == null) target = snap.child("targetClass").getValue(String.class);

                                            if (type == null || target == null) continue;
                                            if (!target.trim().equalsIgnoreCase(studentClass.trim())) continue;

                                            // Subject check: agar item ka subject diya hai, to wo
                                            // student ki enrolled subjects mein bhi hona chahiye.
                                            String itemSubject = snap.child("subject").getValue(String.class);
                                            if (itemSubject != null && !itemSubject.trim().isEmpty()
                                                    && !isEnrolledInSubject(enrolledSubjects, itemSubject)) {
                                                continue;
                                            }

                                            String typeLow = type.toLowerCase();
                                            if (typeLow.contains("quiz"))       quizItems.add(snap);
                                            else if (typeLow.contains("assignment")) assignmentItems.add(snap);
                                        }

                                        // ── Class: Classes node se directly dhundo (upcomingSchedule
                                        // pe depend nahi karna — woh push fail/delay ho sakta hai) ──
                                        loadLiveClassCard(studentClass, enrolledSubjects);

                                        // ── Quiz: earliest wali ek dikhao (submitted wale skip) ──
                                        // Quiz upcomingSchedule mein "date" field nahi hota,
                                        // sirf "time" (jo startDate string hai, time-of-day nahi).
                                        // Isliye dateField "time" pass karo, lekin timeField null
                                        // rakho (warna parseScheduleMillis "17/6/2026" ko time format
                                        // se parse karne ki koshish karega aur ParseException dega).
                                        filterUnattemptedQuizzesAndShow(quizItems);

                                        // ── Assignment: earliest wali ek dikhao (submitted wale skip) ──
                                        // Assignment upcomingSchedule mein "dueDate" aur "dueTime"
                                        // fields save karta hai (na ki "date"/"time"). Isliye correct
                                        // field names pass karo.
                                        filterUnsubmittedAssignmentsAndShow(assignmentItems);
                                    }
                                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                                });
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    /**
     * Check karo kya diya gaya subject student ki enrolled subjects list mein
     * maujood hai (case-insensitive match).
     */
    /**
     * Quiz items mein se woh exclude karo jo student ne already attempt
     * kar diye hain (QuizAttempts/{quizId}/{studentUid} check kar ke),
     * phir baqi (un-attempted) mein se earliest wala card dikhao.
     */
    private void filterUnattemptedQuizzesAndShow(List<DataSnapshot> quizItems) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String studentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        if (quizItems.isEmpty()) {
            showQuizResult(null);
            return;
        }

        DatabaseReference attemptsRef = FirebaseDatabase.getInstance().getReference("QuizAttempts");
        final int[] pending = {quizItems.size()};
        final List<DataSnapshot> unattempted = new ArrayList<>();

        for (DataSnapshot item : quizItems) {
            String quizId = item.child("quizId").getValue(String.class);

            if (quizId == null || quizId.trim().isEmpty()) {
                // Purana record (quizId missing) — attempt check skip karo,
                // bas isay dikhao (warna sab purane quizzes hide ho jayenge)
                unattempted.add(item);
                pending[0]--;
                if (pending[0] == 0) showQuizResult(pickEarliest(unattempted, "time", null));
                continue;
            }

            attemptsRef.child(quizId).child(studentUid)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot attemptSnap) {
                            if (!attemptSnap.exists()) {
                                unattempted.add(item);
                            }
                            pending[0]--;
                            if (pending[0] == 0) {
                                showQuizResult(pickEarliest(unattempted, "time", null));
                            }
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) {
                            unattempted.add(item);
                            pending[0]--;
                            if (pending[0] == 0) {
                                showQuizResult(pickEarliest(unattempted, "time", null));
                            }
                        }
                    });
        }
    }

    private void showQuizResult(DataSnapshot earliestQuiz) {
        if (containerUpcomingQuizzes == null) return;
        containerUpcomingQuizzes.removeAllViews();

        if (earliestQuiz != null) {
            String title = earliestQuiz.child("title").getValue(String.class);
            String date  = earliestQuiz.child("time").getValue(String.class);
            CardView quizCard = addStyledCard(containerUpcomingQuizzes, "Upcoming Quiz",
                    title, "Date: " + date,
                    "#FFEBEE", "#C62828", null);
            quizCard.setOnClickListener(v ->
                    startActivity(new Intent(StudentDashboardActivity.this, QuizActivity.class)));
        } else {
            CardView noQuizCard = addStyledCard(containerUpcomingQuizzes, "Upcoming Quiz",
                    "No Upcoming Quiz", "Check back later",
                    "#FFEBEE", "#C62828", null);
            noQuizCard.setOnClickListener(v ->
                    startActivity(new Intent(StudentDashboardActivity.this, QuizActivity.class)));
        }
    }

    /**
     * Assignment items mein se woh exclude karo jo student ne already submit
     * kar diye hain (Submissions/{assignmentId}/{studentUid} check kar ke),
     * phir baqi (unsubmitted) mein se earliest wala card dikhao.
     */
    private void filterUnsubmittedAssignmentsAndShow(List<DataSnapshot> assignmentItems) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String studentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        if (assignmentItems.isEmpty()) {
            showAssignmentResult(null);
            return;
        }

        DatabaseReference submissionsRef = FirebaseDatabase.getInstance().getReference("Submissions");
        final int[] pending = {assignmentItems.size()};
        final List<DataSnapshot> unsubmitted = new ArrayList<>();

        for (DataSnapshot item : assignmentItems) {
            String assignmentId = item.child("assignmentId").getValue(String.class);

            if (assignmentId == null || assignmentId.trim().isEmpty()) {
                // Purana record (assignmentId missing) — submission check skip karo,
                // bas isay dikhao (warna sab purane assignments hide ho jayenge)
                unsubmitted.add(item);
                pending[0]--;
                if (pending[0] == 0) showAssignmentResult(pickEarliest(unsubmitted, "dueDate", "dueTime"));
                continue;
            }

            submissionsRef.child(assignmentId).child(studentUid)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot subSnap) {
                            if (!subSnap.exists()) {
                                unsubmitted.add(item);
                            }
                            pending[0]--;
                            if (pending[0] == 0) {
                                showAssignmentResult(pickEarliest(unsubmitted, "dueDate", "dueTime"));
                            }
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) {
                            // Error ho to bhi consider karo (filter na karein, taake data hide na ho)
                            unsubmitted.add(item);
                            pending[0]--;
                            if (pending[0] == 0) {
                                showAssignmentResult(pickEarliest(unsubmitted, "dueDate", "dueTime"));
                            }
                        }
                    });
        }
    }

    private void showAssignmentResult(DataSnapshot earliestAssignment) {
        if (containerUpcomingAssignments == null) return;
        containerUpcomingAssignments.removeAllViews();

        if (earliestAssignment != null) {
            String msg     = earliestAssignment.child("message").getValue(String.class);
            String dueDate = earliestAssignment.child("dueDate").getValue(String.class);
            String title   = (msg != null && !msg.trim().isEmpty()) ? msg : "Assignment";
            String detail  = (dueDate != null ? "Due: " + dueDate : "");
            CardView assignCard = addStyledCard(containerUpcomingAssignments, "Assignment",
                    title, detail,
                    "#E8F5E9", "#2E7D32", null);
            assignCard.setOnClickListener(v ->
                    startActivity(new Intent(StudentDashboardActivity.this, AssignmentActivity.class)));
        } else {
            CardView noAssignCard = addStyledCard(containerUpcomingAssignments, "Assignment",
                    "No Upcoming Assignment", "Check back later",
                    "#E8F5E9", "#2E7D32", null);
            noAssignCard.setOnClickListener(v ->
                    startActivity(new Intent(StudentDashboardActivity.this, AssignmentActivity.class)));
        }
    }

    /**
     * Check karo kya diya gaya subject student ki enrolled subjects list mein
     * maujood hai (case-insensitive match).
     */
    private boolean isEnrolledInSubject(List<String> enrolledSubjects, String subject) {
        for (String s : enrolledSubjects) {
            if (s != null && s.trim().equalsIgnoreCase(subject.trim())) return true;
        }
        return false;
    }

    /**
     * List mein se woh item chuno jiski date/time sab se kareeb (earliest) ho,
     * lekin sirf un items mein se jinka time abhi tak guzra nahi (future/now).
     * Guzar chuke (past) items skip ho jaate hain — automatically list se hat jaate hain.
     * Agar koi bhi future item na ho to null return hota hai.
     */
    private DataSnapshot pickEarliest(List<DataSnapshot> items, String dateField, String timeField) {
        if (items.isEmpty()) return null;

        long now = System.currentTimeMillis();
        DataSnapshot best = null;
        long bestMillis = Long.MAX_VALUE;

        for (DataSnapshot item : items) {
            String date = item.child(dateField).getValue(String.class);
            String time = (timeField != null) ? item.child(timeField).getValue(String.class) : null;
            long millis = parseScheduleMillis(date, time);

            // Guzar chuka item (past) skip karo — sirf future/abhi wale consider karo.
            // Agar date/time parse nahi ho saka (millis == MAX_VALUE), tab bhi
            // dikhana behtar hai (info incomplete ho sakti hai), isliye usay rakha jata hai.
            if (millis != Long.MAX_VALUE && millis < now) continue;

            if (millis < bestMillis) {
                best = item;
                bestMillis = millis;
            }
        }
        return best;
    }

    /**
     * Date ("d/M/yyyy") aur time ("hh:mm a") string ko milli-seconds mein convert karo
     * taake comparison/sorting ho sakay. Parse fail hone par Long.MAX_VALUE return hota
     * hai (yani woh item sab se end mein chala jayega).
     *
     * Agar time na diya gaya ho (null/empty), to "end of day" (23:59:59) treat
     * karte hain — start of day (00:00) treat karne se aaj ki date wala item
     * "past" ban jata hai (kyunki midnight hamesha guzar chuka hota hai), jo
     * galat hai. End-of-day fallback se aaj ka item "abhi tak guzra nahi" rehta
     * hai jab tak din khatam na ho.
     */
    private long parseScheduleMillis(String date, String time) {
        try {
            Calendar cal = Calendar.getInstance();

            if (date != null && !date.trim().isEmpty()) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("d/M/yyyy", Locale.getDefault());
                Date d = dateFormat.parse(date.trim());
                if (d != null) {
                    Calendar dCal = Calendar.getInstance();
                    dCal.setTime(d);
                    cal.set(Calendar.YEAR, dCal.get(Calendar.YEAR));
                    cal.set(Calendar.MONTH, dCal.get(Calendar.MONTH));
                    cal.set(Calendar.DAY_OF_MONTH, dCal.get(Calendar.DAY_OF_MONTH));
                }
            }

            if (time != null && !time.trim().isEmpty()) {
                SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.ENGLISH);
                Date t = sdf.parse(time.trim());
                if (t != null) {
                    Calendar tCal = Calendar.getInstance();
                    tCal.setTime(t);
                    cal.set(Calendar.HOUR_OF_DAY, tCal.get(Calendar.HOUR_OF_DAY));
                    cal.set(Calendar.MINUTE, tCal.get(Calendar.MINUTE));
                } else {
                    cal.set(Calendar.HOUR_OF_DAY, 23);
                    cal.set(Calendar.MINUTE, 59);
                }
            } else {
                cal.set(Calendar.HOUR_OF_DAY, 23);
                cal.set(Calendar.MINUTE, 59);
            }
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);

            return cal.getTimeInMillis();
        } catch (ParseException e) {
            return Long.MAX_VALUE;
        }
    }

    /**
     * Classes node se us grade ki class fetch kar ke classLink nikalo.
     * Phir time check karo aur card banao.
     */
    /**
     * Classes node (sab teachers) scan karo, student ke grade + enrolled subject
     * se match karne wali sab classes nikalo, un mein se sab se nazdeek (abhi ya
     * future) wali dhundo aur Live Class card dikhao. Yeh seedha "Classes" node
     * se source hota hai (Class History page jaisa), "upcomingSchedule" pe
     * depend nahi karta — taake push fail/delay hone se card empty na rahe.
     */
    private void loadLiveClassCard(String studentClass, List<String> enrolledSubjects) {
        FirebaseDatabase.getInstance().getReference("Classes")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot allTeachers) {
                        String bestSubject = null, bestTime = null, bestDate = null, bestLink = null;
                        Integer bestDuration = null;
                        long bestMillis = Long.MAX_VALUE;
                        long now = System.currentTimeMillis();

                        for (DataSnapshot teacherSnap : allTeachers.getChildren()) {
                            for (DataSnapshot classSnap : teacherSnap.getChildren()) {
                                String grade   = classSnap.child("grade").getValue(String.class);
                                String sub     = classSnap.child("subject").getValue(String.class);
                                String time    = classSnap.child("time").getValue(String.class);
                                String date    = classSnap.child("date").getValue(String.class);
                                String link    = classSnap.child("classLink").getValue(String.class);
                                Long durLong   = classSnap.child("duration").getValue(Long.class);
                                int duration   = (durLong != null && durLong > 0) ? durLong.intValue() : 45;

                                // Grade match (flexible: "Class 9A" ya "9A")
                                boolean gradeMatch = grade != null && studentClass != null &&
                                        (grade.trim().equalsIgnoreCase(studentClass.trim()) ||
                                                grade.trim().equalsIgnoreCase("Class " + studentClass.trim()));
                                if (!gradeMatch) continue;

                                // Subject match: agar class ka subject diya hai, to wo
                                // student ki enrolled subjects mein hona chahiye.
                                if (sub != null && !sub.trim().isEmpty()
                                        && !isEnrolledInSubject(enrolledSubjects, sub)) continue;

                                long classMillis = parseScheduleMillis(date, time);
                                long windowEnd = classMillis + (duration * 60L * 1000);

                                // Sirf woh classes consider karo jo abhi guzri nahi
                                // (ya to future hain, ya abhi live hain — window end tak)
                                if (classMillis != Long.MAX_VALUE && windowEnd < now) continue;

                                long sortKey = (classMillis != Long.MAX_VALUE) ? classMillis : Long.MAX_VALUE;
                                if (sortKey < bestMillis) {
                                    bestMillis   = sortKey;
                                    bestSubject  = sub;
                                    bestTime     = time;
                                    bestDate     = date;
                                    bestLink     = link;
                                    bestDuration = duration;
                                }
                            }
                        }

                        if (bestSubject != null || bestTime != null) {
                            boolean canJoin = isClassTimeNow(bestTime, bestDate, bestDuration);
                            addClassCard(containerUpcomingClasses,
                                    bestSubject != null ? bestSubject : "Class",
                                    bestTime, bestLink, canJoin);
                        } else {
                            addNoUpcomingClassCard(containerUpcomingClasses);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        addNoUpcomingClassCard(containerUpcomingClasses);
                    }
                });
    }

    /**
     * (Deprecated — ab loadLiveClassCard seedha Classes node use karta hai.
     * Yeh method filhal kahin call nahi hoti, lekin reference ke liye rakhi hai.)
     */
    private void fetchClassLinkAndAddCard(String subject, String classTime, String date, String studentClass) {
        FirebaseDatabase.getInstance().getReference("Classes")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot allTeachers) {
                        String foundLink = null;
                        String foundTime = classTime;
                        String foundDate = date;
                        String foundSubject = subject;
                        Integer foundDuration = null;

                        // Sab teachers ke classes mein dhundo
                        outer:
                        for (DataSnapshot teacherSnap : allTeachers.getChildren()) {
                            for (DataSnapshot classSnap : teacherSnap.getChildren()) {
                                String grade = classSnap.child("grade").getValue(String.class);
                                String sub   = classSnap.child("subject").getValue(String.class);

                                // Grade match karo (flexible: "Class 10A" ya "10A")
                                boolean gradeMatch = grade != null &&
                                        (grade.trim().equalsIgnoreCase(studentClass.trim()) ||
                                                grade.trim().equalsIgnoreCase("Class " + studentClass.trim()));

                                // Subject match (optional agar subject null ho)
                                boolean subMatch = (subject == null || subject.isEmpty()) ||
                                        (sub != null && sub.trim().equalsIgnoreCase(subject.trim()));

                                if (gradeMatch && subMatch) {
                                    foundLink    = classSnap.child("classLink").getValue(String.class);
                                    String t     = classSnap.child("time").getValue(String.class);
                                    String d     = classSnap.child("date").getValue(String.class);
                                    Long durLong = classSnap.child("duration").getValue(Long.class);
                                    if (t != null) foundTime = t;
                                    if (d != null) foundDate = d;
                                    if (sub != null) foundSubject = sub;
                                    if (durLong != null) foundDuration = durLong.intValue();
                                    break outer;
                                }
                            }
                        }

                        // Ab card banao — classLink pass karo (null bhi ho sakta hai)
                        boolean canJoin = isClassTimeNow(foundTime, foundDate, foundDuration);
                        addClassCard(containerUpcomingClasses,
                                foundSubject != null ? foundSubject : "Class",
                                foundTime,
                                foundLink,
                                canJoin);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {
                        // Fallback: link ke baghair card
                        addClassCard(containerUpcomingClasses,
                                subject != null ? subject : "Class",
                                classTime, null, false);
                    }
                });
    }

    /**
     * Check karo kya abhi class ka time hai. Window: class start time se 10 min
     * pehle se shuru ho ke, class ke actual duration (agar diya gaya ho) tak
     * chalta hai. Duration na ho to default 45 min fallback use hota hai.
     * Format expected: "HH:mm AM/PM"  e.g. "10:30 AM"
     */
    private boolean isClassTimeNow(String classTime, String classDate, Integer durationMinutes) {
        if (classTime == null || classTime.isEmpty()) return false;

        try {
            // Today ki date
            SimpleDateFormat dateFormat = new SimpleDateFormat("d/M/yyyy", Locale.getDefault());
            String todayStr = dateFormat.format(new Date());

            // Agar date diya hai aur aaj nahi hai to false
            if (classDate != null && !classDate.isEmpty()) {
                if (!classDate.equalsIgnoreCase(todayStr)) return false;
            }

            // Time parse karo
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.ENGLISH);
            Date classDateTime = sdf.parse(classTime.trim());
            if (classDateTime == null) return false;

            Calendar classCal = Calendar.getInstance();
            Calendar parsedCal = Calendar.getInstance();
            parsedCal.setTime(classDateTime);

            classCal.set(Calendar.HOUR_OF_DAY, parsedCal.get(Calendar.HOUR_OF_DAY));
            classCal.set(Calendar.MINUTE, parsedCal.get(Calendar.MINUTE));
            classCal.set(Calendar.SECOND, 0);

            long classMillis = classCal.getTimeInMillis();
            long nowMillis   = System.currentTimeMillis();

            int duration = (durationMinutes != null && durationMinutes > 0) ? durationMinutes : 45;

            // Join button: class time se 10 min pehle se shuru, aur class ke
            // actual duration ke khatam hote hi (end time pe) gayab ho jata hai.
            long windowStart = classMillis - (10 * 60 * 1000);
            long windowEnd   = classMillis + (duration * 60 * 1000);

            return nowMillis >= windowStart && nowMillis <= windowEnd;

        } catch (ParseException e) {
            return false;
        }
    }

    // --- UI HELPERS ---

    /**
     * Live Class card with dynamic "Join Class" button
     */
    private void addClassCard(LinearLayout container, String subject, String time,
                              String classLink, boolean canJoin) {
        float dp = getResources().getDisplayMetrics().density;

        CardView card = new CardView(this);
        int cardWidth = (int) (180 * dp);
        int cardHeight = (int) (105 * dp); // Sab cards jaisa hi fixed size, Join button aane se size nahi badhta
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(cardWidth, cardHeight);
        params.setMargins(0, 0, (int) (12 * dp), (int) (8 * dp));
        card.setLayoutParams(params);
        card.setRadius(15 * dp);
        card.setCardElevation(6 * dp);
        card.setCardBackgroundColor(Color.parseColor("#E3F2FD"));

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding((int) (15 * dp), (int) (12 * dp), (int) (15 * dp), (int) (12 * dp));
        layout.setClickable(false);
        layout.setFocusable(false);

        // Header — top-left, "Upcoming Quiz" jaisa hi alignment
        TextView tvHeader = new TextView(this);
        tvHeader.setText("Live Class");
        tvHeader.setTextSize(13f);
        tvHeader.setTextColor(Color.parseColor("#1565C0"));
        tvHeader.setTypeface(null, Typeface.BOLD);

        // Subject naam — sirf yeh dikhana hai
        TextView tvTitle = new TextView(this);
        tvTitle.setText(subject);
        tvTitle.setTextSize(15f);
        tvTitle.setTextColor(Color.parseColor("#212121"));
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setSingleLine(true);
        tvTitle.setEllipsize(TextUtils.TruncateAt.END);
        tvTitle.setPadding(0, (int) (4 * dp), 0, 0);

        layout.addView(tvHeader);
        layout.addView(tvTitle);

        // ✅ JOIN CLASS BUTTON — sirf tab jab class ka time ho (chota button, size fix rehta hai)
        if (canJoin) {
            Button btnJoin = new Button(this);
            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, (int) (28 * dp));
            btnParams.setMargins(0, (int) (6 * dp), 0, 0);
            btnJoin.setLayoutParams(btnParams);
            btnJoin.setText("🎥 Join Class");
            btnJoin.setTextSize(10f);
            btnJoin.setTextColor(Color.WHITE);
            btnJoin.setTypeface(null, Typeface.BOLD);
            btnJoin.setPadding(0, 0, 0, 0);
            btnJoin.setMinHeight(0);
            btnJoin.setMinimumHeight(0);
            btnJoin.setStateListAnimator(null);

            // Button background — red gradient
            android.graphics.drawable.GradientDrawable btnBg = new android.graphics.drawable.GradientDrawable();
            btnBg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            btnBg.setCornerRadius(20 * dp);
            btnBg.setColor(Color.parseColor("#D50000"));
            btnJoin.setBackground(btnBg);

            final String link = classLink;
            btnJoin.setOnClickListener(v -> {
                if (link != null && !link.isEmpty()) {
                    openClassLink(link);
                } else {
                    Toast.makeText(this,
                            "Class link not available. Contact to teacher.",
                            Toast.LENGTH_LONG).show();
                }
            });

            layout.addView(btnJoin);
        }

        card.addView(layout);

        // Card pe tap karne se Attendance page khulay (Join button ka click alag hai)
        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(v -> {
            Intent intent = new Intent(this, AttendanceActivity.class);
            intent.putExtra(AttendanceActivity.EXTRA_SUBJECT, subject);
            startActivity(intent);
        });

        container.addView(card);
    }

    /**
     * Google Meet / Zoom / koi bhi link open karo
     */
    private void openClassLink(String link) {
        try {
            if (!link.startsWith("http://") && !link.startsWith("https://")) {
                link = "https://" + link;
            }
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Link cannot open: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private CardView addStyledCard(LinearLayout container, String header, String title,
                                   String detail, String bgColor, String headerColor, String link) {
        float dp = getResources().getDisplayMetrics().density;

        CardView card = new CardView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams((int) (180 * dp), (int) (105 * dp));
        params.setMargins(0, 0, (int) (12 * dp), (int) (8 * dp));
        card.setLayoutParams(params);
        card.setRadius(15 * dp);
        card.setCardElevation(6 * dp);
        card.setCardBackgroundColor(Color.parseColor(bgColor));

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding((int) (15 * dp), (int) (12 * dp), (int) (15 * dp), (int) (12 * dp));

        TextView tvHeader = new TextView(this);
        tvHeader.setText(header);
        tvHeader.setTextSize(13f);
        tvHeader.setTextColor(Color.parseColor(headerColor));
        tvHeader.setTypeface(null, Typeface.BOLD);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextSize(17f);
        tvTitle.setTextColor(Color.parseColor("#212121"));
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setSingleLine(true);
        tvTitle.setEllipsize(TextUtils.TruncateAt.END);
        tvTitle.setPadding(0, (int) (4 * dp), 0, 0);

        TextView tvDetail = new TextView(this);
        tvDetail.setText(detail);
        tvDetail.setTextSize(12f);
        tvDetail.setTextColor(Color.parseColor("#616161"));

        layout.addView(tvHeader);
        layout.addView(tvTitle);
        layout.addView(tvDetail);
        card.addView(layout);
        container.addView(card);
        return card;
    }

    /**
     * "No Upcoming Class" card — Live Class card jaisa hi design (blue theme, same
     * header), sirf andar message "No Upcoming Class" hota hai aur Join button nahi hota.
     */
    private void addNoUpcomingClassCard(LinearLayout container) {
        float dp = getResources().getDisplayMetrics().density;

        CardView card = new CardView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                (int) (180 * dp), (int) (105 * dp));
        params.setMargins(0, 0, (int) (12 * dp), (int) (8 * dp));
        card.setLayoutParams(params);
        card.setRadius(15 * dp);
        card.setCardElevation(6 * dp);
        card.setCardBackgroundColor(Color.parseColor("#E3F2FD"));

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding((int) (12 * dp), (int) (10 * dp), (int) (12 * dp), (int) (10 * dp));

        // Header — same as Live Class
        TextView tvHeader = new TextView(this);
        tvHeader.setText("Live Class");
        tvHeader.setTextSize(12f);
        tvHeader.setTextColor(Color.parseColor("#1565C0"));
        tvHeader.setTypeface(null, Typeface.BOLD);

        // Title
        TextView tvTitle = new TextView(this);
        tvTitle.setText("No Upcoming Class");
        tvTitle.setTextSize(15f);
        tvTitle.setTextColor(Color.parseColor("#212121"));
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setSingleLine(true);
        tvTitle.setEllipsize(TextUtils.TruncateAt.END);
        tvTitle.setPadding(0, (int) (3 * dp), 0, 0);

        // Detail
        TextView tvDetail = new TextView(this);
        tvDetail.setText("Check back later");
        tvDetail.setTextSize(11f);
        tvDetail.setTextColor(Color.parseColor("#616161"));

        layout.addView(tvHeader);
        layout.addView(tvTitle);
        layout.addView(tvDetail);

        card.addView(layout);

        // Card pe tap karne se Attendance page khulay (overall summary)
        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(v -> startActivity(new Intent(this, AttendanceActivity.class)));

        container.addView(card);
    }

    private void filterCards(String query) {
        if (query.isEmpty()) {
            for (CardView card : allCards) card.setVisibility(View.VISIBLE);
            return;
        }
        String lowerQuery = query.toLowerCase();
        for (int i = 0; i < allCards.size(); i++) {
            String label = allCardLabels.get(i).toLowerCase();
            allCards.get(i).setVisibility(label.contains(lowerQuery) ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_logout) {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(this, LoginSelectionActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}