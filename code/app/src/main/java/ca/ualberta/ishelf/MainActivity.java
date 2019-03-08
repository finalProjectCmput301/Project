package ca.ualberta.ishelf;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.app.SearchManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.UUID;


/**
 * MainActivity
 *
 * - mehrab edit
 */

public class MainActivity extends AppCompatActivity {

    // Here is my first comment
    private TextView mTextMessage;
    private Toolbar myToolbar;
    private SearchView searchView;
    private TextView appName;
    private ImageView profileIcon;


    private static final String TAG = "MainActivity";

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            Fragment selectedFragment = null;

            switch (item.getItemId()) {
                case R.id.my_books:
                    selectedFragment = new myBooksFragment();
                    break;
                case R.id.borrow_books:
                    selectedFragment = new BorrowFragment();
                    //selectedFragment = new FavoritesFragment();
                    break;
                case R.id.request_books:
                    selectedFragment = new RequestFragment();
                    break;
            }

            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                    selectedFragment).commit();

            return true;
        }
        };

    Fragment fragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SignIn();


        mTextMessage = (TextView) findViewById(R.id.message);

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        appName = (TextView) findViewById(R.id.app_name);
        profileIcon = (ImageView) findViewById(R.id.profile_icon);


        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);


        searchView = (SearchView) findViewById(R.id.searchView1);
        searchView.setQueryHint("Search for Books");





        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                appName.setVisibility(View.INVISIBLE);
                profileIcon.setVisibility(View.INVISIBLE);
            }
        });

        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                appName.setVisibility(View.VISIBLE);
                profileIcon.setVisibility(View.VISIBLE);
                return false;
            }
        });



        // for back button later
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//        getSupportActionBar().setDisplayShowHomeEnabled(true);


        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                    new BorrowFragment()).commit();
        }



        /**
         * To Test viewing profile uncomment set the if statement to true
         */
//        if(false) {
//            findViewById(R.id.button1).setVisibility(View.GONE);
//            findViewById(R.id.button1).setVisibility(View.GONE);
//            findViewById(R.id.button1).setVisibility(View.GONE);
//        }


        //loadFragment(new myBooksFragment());
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.toolbar, menu);
//        return true;
//    }

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//            case R.id.action_favorite:
//                // User chose the "Settings" item, show the app settings UI...
//                Intent intent = new Intent(this, ViewProfileActivity.class);
//                String username = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE).getString("username", "TestUsername");
//                Log.d(TAG, "onOptionsItemSelected: Username:" + username);
//                intent.putExtra("Username", username);
//                startActivity(intent);
//                finish();
//                return true;
//
//            case R.id.action_settings:
//                // User chose the "Favorite" action, mark the current item
//                // as a favorite...
//                return true;
//
//            default:
//                // If we got here, the user's action was not recognized.
//                // Invoke the superclass to handle it.
//                return super.onOptionsItemSelected(item);
//
//        }
//    }





//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.search_menu, menu);
//
//        MenuItem searchItem = menu.findItem(R.id.book_search);
//        SearchView searchView = (SearchView) searchItem.getActionView();
//
//        return true;
//    }

//    @Override
//    public boolean onQueryTextChange(String query) {
//        // Here is where we are going to implement the filter logic
//        return false;
//    }
//
//    @Override
//    public boolean onQueryTextSubmit(String query) {
//        return false;
//    }





//    public boolean onCreateOptionsMenu(Menu menu) {
//
//        final MenuItem searchItem = menu.findItem(R.id.book_search);
//        final android.support.v7.widget.SearchView searchView = (SearchView) searchItem.getActionView();
//        searchView.setQueryHint("test for something");
//
//        return true;
//    }


    private void SignIn(){

        // code to reset username in UserPreferences
        SharedPreferences.Editor editor = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE).edit();
        editor.putString("username", null).apply();

        // Check if logged-in
        String username = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE).getString("username", null);
        if (username == null) {     // user is not logged-in
            Intent intent = new Intent(this, SignInActivity.class);
            startActivityForResult(intent, 1);
        }
    }


    /**
     * When the profile icon is hit on the appbar
     * Takes the user to the ViewProfileActivity
     * @author Jeremy
     * @param v
     */
    public void ViewProfile(View v){
        Log.d(TAG, "ViewProfile: button clicked");
        Intent intent = new Intent(v.getContext(), ViewProfileActivity.class);
        String username = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE).getString("username", "TestUsername");
        Log.d(TAG, "onOptionsItemSelected: Username:" + username);
        intent.putExtra("Username", username);
        startActivity(intent);
    }

//    public void button1(View v){
//        User user1 = new User();
//        user1.setUsername("User1_username");
//        user1.setPhoneNum("809-888-1234");
//        user1.setEmail("user1@test.com");
//
//        Intent intent = new Intent(this, ViewProfileActivity.class);
//        intent.putExtra("User", user1);
//        startActivity(intent);
//    }
//
//    public void button2(View v){
//        User user2 = new User();
//        user2.setUsername("ABC");
//        user2.setPhoneNum("222-222-2222");
//        user2.setEmail("user2@t22est.com");
//
//        Intent intent = new Intent(this, ViewProfileActivity.class);
//        intent.putExtra("User", user2);
//        startActivity(intent);
//    }
//
//    public void button3(View v){
//        Intent intent = new Intent(this, ViewProfileActivity.class);
//        //intent.putExtra("User", user3);
//        intent.putExtra("Username",
//                getSharedPreferences("UserPreferences", Context.MODE_PRIVATE).getString("username", null));
//        startActivity(intent);
//
//    }
}
