package ravtrix.foodybuddy.activities.mainpage;

import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import ravtrix.foodybuddy.R;
import ravtrix.foodybuddy.activities.drawerrecycler.adapter.DrawerRecyclerAdapter;
import ravtrix.foodybuddy.activities.drawerrecycler.model.DrawerModel;
import ravtrix.foodybuddy.activities.mainpage.model.EventJoined;
import ravtrix.foodybuddy.decorator.DividerDecoration;
import ravtrix.foodybuddy.fragments.deals.DealsFragment;
import ravtrix.foodybuddy.fragments.inbox.InboxFragment;
import ravtrix.foodybuddy.fragments.maineventfrag.IOnDistanceSettingSelected;
import ravtrix.foodybuddy.fragments.maineventfrag.MainEventFrag;
import ravtrix.foodybuddy.fragments.userprofilefrag.UserProfileFrag;
import ravtrix.foodybuddy.localstore.UserLocalStore;
import ravtrix.foodybuddy.utils.Helpers;
import ravtrix.foodybuddy.utils.RetrofitEventSingleton;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    @BindView(R.id.viewpager) protected ViewPager viewPager;
    @BindView(R.id.tabs) protected TabLayout tabLayout;
    @BindView(R.id.toolbar) protected Toolbar toolbar;
    @BindView(R.id.activity_main_drawer_layout) protected DrawerLayout drawerLayout;
    @BindView(R.id.acitivty_main_nav_view) protected NavigationView navigationView;
    @BindView(R.id.activity_main_recyclerView) protected RecyclerView recyclerViewMain;
    @BindView(R.id.activity_main_tvUpcoming) protected TextView tvUpcomingEvents;
    @BindView(R.id.activity_main_layoutEdit) protected LinearLayout layoutEdit;
    private ImageView imageSetting, imageNavigation;
    private ViewPagerAdapter adapter;
    private List<DrawerModel> drawerModels;
    private DrawerRecyclerAdapter drawerRecyclerAdapter;
    private boolean isEventEditClicked = false;
    private UserLocalStore userLocalStore;
    private IOnDistanceSettingSelected iOnDistanceSettingSelected;
    private CompositeSubscription mSubscriptions;
    private List<EventJoined> eventModels;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        Helpers.overrideFonts(this, tvUpcomingEvents);
        Helpers.overrideFonts(this, layoutEdit);


        // Set views
        TextView toolbarTitle = (TextView) toolbar.findViewById(R.id.toolbar_title);
        imageNavigation = (ImageView) toolbar.findViewById(R.id.layout_main_imageNavigation);
        imageSetting = (ImageView) toolbar.findViewById(R.id.layout_main_imageSetting);

        // Set font
        toolbarTitle.setTypeface(Typeface.createFromAsset(getAssets(), "toolbar2.ttf"));

        setupViewPager(); // set adapter with data
        viewPager.setOffscreenPageLimit(3); // define size of tabs
        tabLayout.setupWithViewPager(viewPager); // push viewpager into the tab layout

        // Set up tabs and view-pager
        setTabIcons();
        setTabLayoutListener();
        setViewPagerListener();

        // set button listeners
        imageNavigation.setOnClickListener(this);
        imageSetting.setOnClickListener(this);
        layoutEdit.setOnClickListener(this);
        userLocalStore = new UserLocalStore(this);

        RecyclerView.ItemDecoration dividerDecorator = new DividerDecoration(this, R.drawable.line_divider_drawer);
        recyclerViewMain.addItemDecoration(dividerDecorator);
        mSubscriptions = new CompositeSubscription();


        fetchDrawerModelsRetrofit();

        /*
        drawerModels = new ArrayList<>();
        DrawerModel drawerModel1 = new DrawerModel("Lucky Jams", "4 more days", "1322 Yakima");
        DrawerModel drawerModel2 = new DrawerModel("Curry Friends", "11 more days", "810 New Street");
        DrawerModel drawerModel3 = new DrawerModel("Kit Kat", "14 more days", "1022 Jumper Ave");
        drawerModels.add(drawerModel1);
        drawerModels.add(drawerModel2);
        drawerModels.add(drawerModel3);*/
    }

    private void fetchDrawerModelsRetrofit() {

        mSubscriptions.add(RetrofitEventSingleton.getRetrofitEvent()
                .getEventJoined()
                .getEventJoined(userLocalStore.getLoggedInUser().getUserID())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(new Observer<List<EventJoined>>() {
                    @Override
                    public void onCompleted() {}

                    @Override
                    public void onError(Throwable e) {}

                    @Override
                    public void onNext(List<EventJoined> events) {
                        // Fetch events joined
                        eventModels = events;
                        drawerRecyclerAdapter = new DrawerRecyclerAdapter(MainActivity.this, eventModels);
                        recyclerViewMain.setAdapter(drawerRecyclerAdapter);
                        recyclerViewMain.setLayoutManager(new LinearLayoutManager(MainActivity.this));
                    }
                }));
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.layout_main_imageNavigation:
                drawerLayout.openDrawer(navigationView);
                break;
            case R.id.layout_main_imageSetting:
                // call fragment to update distance

                AlertDialog.Builder b = new AlertDialog.Builder(this);
                b.setTitle("Filter by Distance");
                final String[] types = getResources().getStringArray(R.array.distance_main_spinner);
                b.setItems(types, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        iOnDistanceSettingSelected.onDistanceSelected(types[which]);

                    }
                });
                b.setCancelable(true);
                b.show();
                break;
            case R.id.activity_main_layoutEdit:
                if (!isEventEditClicked) {
                    drawerRecyclerAdapter.setEditEventClicked(true);
                    isEventEditClicked = true;
                } else {
                    // User already clicked edit before, if they click again, it means they want to cancel edit
                    drawerRecyclerAdapter.setEditEventClicked(false);
                    isEventEditClicked = false;
                }
                break;
            default:
                break;
        }
    }

    private void lockDrawerLayoutScroll() {
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED); // lock from swipe
    }

    private void hideMainToolbarButtons() {
        imageSetting.setVisibility(View.INVISIBLE);
    }

    private void showMainToolbarButtons() {
        imageSetting.setVisibility(View.VISIBLE);
    }

    /**
     * Set up the tab adapter with fragments
     */
    private void setupViewPager() {

        adapter = new ViewPagerAdapter(getSupportFragmentManager());
        Fragment mainEventFrag = new MainEventFrag();
        // Set adapter with different fragments and their titles
        adapter.addFragment(mainEventFrag);
        adapter.addFragment(new InboxFragment()); // inbox
        adapter.addFragment(new UserProfileFrag());
        adapter.addFragment(new DealsFragment());
        this.viewPager.setAdapter(adapter);
        this.iOnDistanceSettingSelected = (IOnDistanceSettingSelected) mainEventFrag;
    }

    /**
     * Set up icons for different tabs
     */
    private void setTabIcons() {
        for (int i = 0; i < tabLayout.getTabCount(); i++) {
            switch (i) {
                case 0:
                    tabLayout.getTabAt(i).setIcon(R.drawable.ic_home);
                    break;
                case 1:
                    tabLayout.getTabAt(i).setIcon(R.drawable.ic_inbox);
                    break;
                case 2:
                    tabLayout.getTabAt(i).setIcon(R.drawable.ic_user);
                    break;
                case 3:
                    tabLayout.getTabAt(i).setIcon(R.drawable.ic_price_tag);
                    break;
            }
        }
    }

    /**
     * Listens for when the user "clicks" between tabs or re-clicks the tabs
     */
    private void setTabLayoutListener() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {}
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // This is when the user re-clicks the main tab button. This should scroll the recycler view
                // back to the top
                if (tab.getPosition() == 0) {
                    ((MainEventFrag) adapter.getFragmentAtPosition(0)).scrollToTop();
                }
            }
        });
    }

    /**
     * Listens for when the user scrolls or clicks between tabs
     */
    private void setViewPagerListener() {
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

            @Override
            public void onPageSelected(int position) {
                switch (position) {
                    case 0:
                        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
                        showMainToolbarButtons();
                        break;
                    case 1:
                        // Load data when tab is visible
                        //((InboxFragment) adapter.getFragmentAtPosition(1)).loadViewWithData();
                        // Lock swipe
                        lockDrawerLayoutScroll();
                        // Hide toolbar items
                        hideMainToolbarButtons();
                        break;
                    case 2:
                        //((UserProfileFrag) adapter.getFragmentAtPosition(2)).loadViewWithData();
                        lockDrawerLayoutScroll();
                        hideMainToolbarButtons();
                        break;
                    case 3:
                        lockDrawerLayoutScroll();
                        break;
                    default:
                        break;
                }
            }
            @Override
            public void onPageScrollStateChanged(int state) {}
        });
    }

    /**
     * Adapter class for custom tabs
     */
    class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>(); // array list with tab fragments

        ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        void addFragment(Fragment fragment) {
            mFragmentList.add(fragment);
        }

        Fragment getFragmentAtPosition(int position) {
            return mFragmentList.get(position);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mSubscriptions.unsubscribe();
    }
}
