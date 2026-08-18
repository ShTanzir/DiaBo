package com.tanzir.diabo.templates

data class ProjectTemplate(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val javaContent: String,
    val xmlContent: String
)

/**
 * Curated starter templates per PRD §7.7. Picking one seeds a new project's
 * MainActivity.java + activity_main.xml instead of the bare "Hello, DiaBo!" default.
 */
object TemplateCatalog {

    val all: List<ProjectTemplate> = listOf(
        ProjectTemplate(
            id = "blank",
            title = "Blank Activity",
            description = "A single empty screen — the same default DiaBo already seeds.",
            category = "Basics",
            javaContent = """
                package {{PACKAGE}};

                import android.os.Bundle;
                import androidx.appcompat.app.AppCompatActivity;

                public class MainActivity extends AppCompatActivity {
                    @Override
                    protected void onCreate(Bundle savedInstanceState) {
                        super.onCreate(savedInstanceState);
                        setContentView(R.layout.activity_main);
                    }
                }
            """.trimIndent(),
            xmlContent = """
                <?xml version="1.0" encoding="utf-8"?>
                <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
                    android:layout_width="match_parent"
                    android:layout_height="match_parent"
                    android:orientation="vertical"
                    android:gravity="center">

                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="Hello, DiaBo!"
                        android:textSize="20sp" />

                </LinearLayout>
            """.trimIndent()
        ),
        ProjectTemplate(
            id = "login",
            title = "Login Screen",
            description = "Username/password fields with a login button and a Toast on click.",
            category = "UI Patterns",
            javaContent = """
                package {{PACKAGE}};

                import android.os.Bundle;
                import android.view.View;
                import android.widget.EditText;
                import android.widget.Toast;
                import androidx.appcompat.app.AppCompatActivity;

                public class MainActivity extends AppCompatActivity {
                    @Override
                    protected void onCreate(Bundle savedInstanceState) {
                        super.onCreate(savedInstanceState);
                        setContentView(R.layout.activity_main);
                    }

                    public void onLoginClick(View v) {
                        Toast.makeText(this, "Login tapped", Toast.LENGTH_SHORT).show();
                    }
                }
            """.trimIndent(),
            xmlContent = """
                <?xml version="1.0" encoding="utf-8"?>
                <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
                    android:layout_width="match_parent"
                    android:layout_height="match_parent"
                    android:orientation="vertical"
                    android:gravity="center"
                    android:padding="24dp">

                    <EditText
                        android:id="@+id/usernameInput"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:hint="Username" />

                    <EditText
                        android:id="@+id/passwordInput"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:hint="Password" />

                    <Button
                        android:id="@+id/loginButton"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:text="Log In"
                        android:onClick="onLoginClick" />

                </LinearLayout>
            """.trimIndent()
        ),
        ProjectTemplate(
            id = "list",
            title = "List (RecyclerView-style)",
            description = "A scrollable list of items — renders as a placeholder in Instant Preview, full accuracy in Real Build.",
            category = "UI Patterns",
            javaContent = """
                package {{PACKAGE}};

                import android.os.Bundle;
                import androidx.appcompat.app.AppCompatActivity;

                public class MainActivity extends AppCompatActivity {
                    @Override
                    protected void onCreate(Bundle savedInstanceState) {
                        super.onCreate(savedInstanceState);
                        setContentView(R.layout.activity_main);
                    }
                }
            """.trimIndent(),
            xmlContent = """
                <?xml version="1.0" encoding="utf-8"?>
                <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
                    android:layout_width="match_parent"
                    android:layout_height="match_parent"
                    android:orientation="vertical">

                    <TextView
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:text="My List"
                        android:textSize="22sp"
                        android:padding="16dp" />

                    <androidx.recyclerview.widget.RecyclerView
                        android:id="@+id/recyclerView"
                        android:layout_width="match_parent"
                        android:layout_height="match_parent" />

                </LinearLayout>
            """.trimIndent()
        ),
        ProjectTemplate(
            id = "bottom_nav",
            title = "Bottom Navigation",
            description = "A tabbed shell with a bottom nav bar placeholder.",
            category = "Navigation",
            javaContent = """
                package {{PACKAGE}};

                import android.os.Bundle;
                import androidx.appcompat.app.AppCompatActivity;

                public class MainActivity extends AppCompatActivity {
                    @Override
                    protected void onCreate(Bundle savedInstanceState) {
                        super.onCreate(savedInstanceState);
                        setContentView(R.layout.activity_main);
                    }
                }
            """.trimIndent(),
            xmlContent = """
                <?xml version="1.0" encoding="utf-8"?>
                <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
                    android:layout_width="match_parent"
                    android:layout_height="match_parent"
                    android:orientation="vertical">

                    <FrameLayout
                        android:layout_width="match_parent"
                        android:layout_height="0dp"
                        android:layout_weight="1">

                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:layout_gravity="center"
                            android:text="Screen content here" />
                    </FrameLayout>

                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="56dp"
                        android:orientation="horizontal"
                        android:gravity="center"
                        android:background="#EEEEEE">

                        <TextView
                            android:layout_width="0dp"
                            android:layout_weight="1"
                            android:layout_height="wrap_content"
                            android:text="Home"
                            android:gravity="center" />

                        <TextView
                            android:layout_width="0dp"
                            android:layout_weight="1"
                            android:layout_height="wrap_content"
                            android:text="Search"
                            android:gravity="center" />

                        <TextView
                            android:layout_width="0dp"
                            android:layout_weight="1"
                            android:layout_height="wrap_content"
                            android:text="Profile"
                            android:gravity="center" />

                    </LinearLayout>
                </LinearLayout>
            """.trimIndent()
        ),
        ProjectTemplate(
            id = "settings",
            title = "Settings Screen",
            description = "A simple vertical list of labeled options.",
            category = "UI Patterns",
            javaContent = """
                package {{PACKAGE}};

                import android.os.Bundle;
                import androidx.appcompat.app.AppCompatActivity;

                public class MainActivity extends AppCompatActivity {
                    @Override
                    protected void onCreate(Bundle savedInstanceState) {
                        super.onCreate(savedInstanceState);
                        setContentView(R.layout.activity_main);
                    }
                }
            """.trimIndent(),
            xmlContent = """
                <?xml version="1.0" encoding="utf-8"?>
                <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
                    android:layout_width="match_parent"
                    android:layout_height="match_parent"
                    android:orientation="vertical"
                    android:padding="16dp">

                    <TextView
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:text="Notifications"
                        android:padding="12dp" />

                    <TextView
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:text="Appearance"
                        android:padding="12dp" />

                    <TextView
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:text="About"
                        android:padding="12dp" />

                </LinearLayout>
            """.trimIndent()
        ),
        ProjectTemplate(
            id = "form",
            title = "Form with Validation",
            description = "A short form with a submit button wired to a Toast confirmation.",
            category = "UI Patterns",
            javaContent = """
                package {{PACKAGE}};

                import android.os.Bundle;
                import android.view.View;
                import android.widget.Toast;
                import androidx.appcompat.app.AppCompatActivity;

                public class MainActivity extends AppCompatActivity {
                    @Override
                    protected void onCreate(Bundle savedInstanceState) {
                        super.onCreate(savedInstanceState);
                        setContentView(R.layout.activity_main);
                    }

                    public void onSubmitClick(View v) {
                        Toast.makeText(this, "Form submitted", Toast.LENGTH_SHORT).show();
                    }
                }
            """.trimIndent(),
            xmlContent = """
                <?xml version="1.0" encoding="utf-8"?>
                <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
                    android:layout_width="match_parent"
                    android:layout_height="match_parent"
                    android:orientation="vertical"
                    android:padding="24dp">

                    <EditText
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:hint="Full name" />

                    <EditText
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:hint="Email" />

                    <Button
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:text="Submit"
                        android:onClick="onSubmitClick" />

                </LinearLayout>
            """.trimIndent()
        )
    )

    fun categories(): List<String> = all.map { it.category }.distinct()
}
