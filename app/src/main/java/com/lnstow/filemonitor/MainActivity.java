package com.lnstow.filemonitor;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.preference.EditTextPreference;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "123123";
    FileListener listener;
    Handler handler;
    SettingsFragment settingsFragment;
    DrawerLayout drawerLayout;
    RecyclerView recyclerView;
    StringAdapter adapter;
    LinearLayoutManager layoutManager;
//    AppCompatTextView textFileListen;
//    NestedScrollView nestedScrollView;
//    ScrollListener scrollListener;
//    HorizontalScrollView scrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        handler = new mHandler(this);
        settingsFragment = new SettingsFragment();
        drawerLayout = findViewById(R.id.drawer);
//        nestedScrollView = findViewById(R.id.scroll);
//        textFileListen.setMovementMethod(ScrollingMovementMethod.getInstance());
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
//        String[] defMask = getResources().getStringArray(R.array.mask_values);
        String[] defMask = {"00000002", "00000018", "000008C0", "00000100", "00000600"};
        String path = sp.getString("path", null);
        String color = sp.getString("color", "#8A6BBE");
        Set<String> maskSet = sp.getStringSet("mask", new HashSet<>(Arrays.asList(defMask)));
        int mask = 0;
        if (path == null) {
            SharedPreferences.Editor editor = sp.edit();
            path = Environment.getExternalStorageDirectory().getAbsolutePath();
            editor.putString("path", path);
            editor.putString("color", color);
            editor.putStringSet("mask", maskSet);
            editor.apply();
        }
        for (String s : maskSet) {
            mask |= Integer.parseInt(s, 16);
        }

        listener = new FileListener(path, mask, handler,
                sp.getString("exclude", "").split("\n"));
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission
                .READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new
                    String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 111);
        } else {
            listener.startListen();
        }

        layoutManager = new LinearLayoutManager(this);
//        layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        adapter = new StringAdapter(FileListener.FullInfo, Color.parseColor(color));
        recyclerView = findViewById(R.id.recycler);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
//        recyclerView.setNestedScrollingEnabled(true);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, settingsFragment)
                .commit();
//        ActionBar actionBar = getSupportActionBar();
//        if (actionBar != null) {
//            actionBar.setDisplayHomeAsUpEnabled(true);
//        }

//        scrollListener = new ScrollListener(nestedScrollView, textFileListen);
//        nestedScrollView.setOnScrollChangeListener(scrollListener);
//        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
//            @Override
//            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
//                super.onScrolled(recyclerView, dx, dy);
//                Log.d(TAG, "onScrolled: " + dx + dy);
//            }
//        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        listener.stopListen();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 111:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    listener.startListen();
                } else {
                    Toast.makeText(this, "没有权限", Toast.LENGTH_SHORT).show();
                }
                Message message = Message.obtain();
                message.what = 14;
                message.arg1 = 1;
                handler.sendMessageDelayed(message, 1500);
                message = Message.obtain();
                message.what = 14;
                message.arg1 = 0;
                handler.sendMessageDelayed(message, 2500);
                break;
            default:
                break;
        }
    }


    public static class mHandler extends Handler {
        WeakReference<MainActivity> weakReference;
        //        AtomicInteger num = new AtomicInteger(0);

        mHandler(MainActivity mainActivity) {
            weakReference = new WeakReference<>(mainActivity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            MainActivity mainActivity = weakReference.get();
            if (mainActivity == null) {
                return;
            }
            switch (msg.what) {
                case 18:
//                    System.out.println("testunint  " + num.addAndGet(1) + "  " + FileListener.number);
                    mainActivity.adapter.setLen(msg.arg2);
                    mainActivity.adapter.notifyItemRangeChanged(msg.arg1, msg.arg2 - msg.arg1);
                    break;

                case 16:
                    Toast.makeText(mainActivity, msg.obj.toString(), Toast.LENGTH_SHORT).show();
                    mainActivity.adapter.setData(FileListener.FullInfo);
                    break;
                case 14:
                    if (msg.arg1 == 1) {
                        mainActivity.drawerLayout.openDrawer(GravityCompat.START);
                    }
                    if (msg.arg1 == 0) {
                        mainActivity.drawerLayout.closeDrawer(GravityCompat.START);
                    }
                    break;

                case 12:
                    mainActivity.adapter.setData((String[]) msg.obj);
                    mainActivity.adapter.setLen(msg.arg1);
                    mainActivity.adapter.notifyDataSetChanged();
                    FileListener.flagSend = false;
                    break;
                case 10:
                    Toast.makeText(mainActivity, msg.obj.toString(), Toast.LENGTH_LONG).show();
                    break;

                default:
                    break;
            }

        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        Preference githubPreference;
        Preference refPreference;
        EditTextPreference pathPreference;
        EditTextPreference colorPreference;
        MultiSelectListPreference maskPreference;
        EditTextPreference searchPreference;
        EditTextPreference excludePreference;
        Preference.OnPreferenceClickListener showToast = new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Toast.makeText(getContext(), "长按复制", Toast.LENGTH_SHORT).show();
                return false;
            }
        };
        Preference.OnPreferenceChangeListener checkPrefs = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final String valueString = newValue.toString();
                if (preference == pathPreference) {
                    if (valueString.charAt(valueString.length() - 1) == '/') {
                        Toast.makeText(getContext(), "结尾不应带/", Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    if (new File(valueString).canRead()) {
                        Toast.makeText(getContext(), "重启应用生效", Toast.LENGTH_SHORT).show();
                        return true;
                    } else {
                        Toast.makeText(getContext(), "不正确目录", Toast.LENGTH_SHORT).show();
                        return false;
                    }
                }
                if (preference == colorPreference) {
//                    AppCompatTextView textView = getActivity().findViewById(R.id.text_file_listen);
//                    textView.setTextColor(0xAABBCCDD);
                    int color;
                    try {
                        color = Color.parseColor(valueString);
                    } catch (IllegalArgumentException | StringIndexOutOfBoundsException e) {
                        Toast.makeText(getContext(), "不正确颜色", Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    MainActivity activity = (MainActivity) getActivity();
                    int pos = activity.layoutManager.findFirstVisibleItemPosition();
                    int len = activity.layoutManager.findLastVisibleItemPosition() - pos;
                    activity.adapter.setColor(color);
                    activity.adapter.notifyItemRangeChanged(pos, len);

                    return true;
                }
                if (preference == maskPreference) {
                    Toast.makeText(getContext(), "重启应用生效", Toast.LENGTH_SHORT).show();
                    return true;
                }
                if (preference == searchPreference) {
                    MainActivity activity = (MainActivity) getActivity();
                    if (valueString.length() == 0) {
                        if (activity.adapter.getData() != FileListener.FullInfo) {
                            activity.adapter.setData(FileListener.FullInfo);
                            activity.adapter.setLen(FileListener.number);
                            activity.adapter.notifyDataSetChanged();
                            FileListener.flagSend = true;
                        }
                    } else {
                        FileListener.singleThread.execute(() -> {
                            String[] result = new String[FileListener.number];
                            int len = MainActivity.fastFind(valueString, result);
                            Message message = Message.obtain();
                            message.what = 12;
                            message.obj = result;
                            message.arg1 = len;
                            FileListener.handler.sendMessage(message);
                        });
                    }
                    activity.drawerLayout.closeDrawer(GravityCompat.START);
                    return false;
                }
                if (preference == excludePreference) {
                    if (valueString.charAt(valueString.length() - 1) == '\n') {
                        Toast.makeText(getContext(), "结尾不应换行", Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    String[] excludeDir = valueString.split("\n");
                    for (String str : excludeDir) {
                        if (!new File(str).canRead()) {
                            Toast.makeText(getContext(), "不正确目录", Toast.LENGTH_SHORT).show();
                            return false;
                        }
                    }
                    Toast.makeText(getContext(), "重启应用生效", Toast.LENGTH_SHORT).show();
                    return true;
                }
                return false;
            }
        };

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.prefs, rootKey);
            githubPreference = findPreference("github");
            refPreference = findPreference("ref");
            pathPreference = findPreference("path");
            colorPreference = findPreference("color");
            maskPreference = findPreference("mask");

            githubPreference.setOnPreferenceClickListener(showToast);
            refPreference.setOnPreferenceClickListener(showToast);
            pathPreference.setOnPreferenceChangeListener(checkPrefs);
            colorPreference.setOnPreferenceChangeListener(checkPrefs);
            maskPreference.setOnPreferenceChangeListener(checkPrefs);

            searchPreference = findPreference("search");
            searchPreference.setOnPreferenceChangeListener(checkPrefs);
            excludePreference = findPreference("exclude");
            excludePreference.setOnPreferenceChangeListener(checkPrefs);
        }

    }

    private static int fastFind(String subStr, String[] result) {
        int len = 0;
        int maxSize = result.length;
        for (int i = 0; i < maxSize; i++) {
            if (FileListener.FullInfo[i].indexOf(subStr) >= 0) {
                result[len++] = FileListener.FullInfo[i];
            }
        }
        return len;
    }

//    public static class ScrollListener implements NestedScrollView.OnScrollChangeListener {
//        //        int px_10dp = 10 * getResources().getDisplayMetrics().densityDpi / 160;//px=dp*dpi/160
//        int beginPos = 0;
//        boolean flagScroll = false;
//        //        WeakReference<MainActivity> mainActivityWeakReference;
////        WeakReference<NestedScrollView> scrollViewWeakReference;
//        WeakReference<AppCompatTextView> textViewWeakReference;
//
//        ScrollListener(NestedScrollView scrollView, AppCompatTextView textView) {
////            this.scrollViewWeakReference = new WeakReference<>(scrollView);
//            this.textViewWeakReference = new WeakReference<>(textView);
//        }
//
//        @Override
//        public void onScrollChange(NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
//            StringBuilder stringBuilder = new StringBuilder();
//            stringBuilder.append(scrollX).append("  ").append(scrollY).append("  ")
//                    .append(oldScrollX).append("  ").append(oldScrollY);
//            System.out.println(stringBuilder);
////            System.out.println(v.getMeasuredHeight());
////            System.out.println(v.getChildAt(0).getMeasuredHeight());
////            System.out.println(textViewWeakReference.get().getMeasuredHeight());
//            if (FileListener.number < 600) return;
////            if (scrollY - oldScrollY > px_10dp && t2 - t1 > 500) {
////            下滑
////                    System.out.println(stringBuilder + "down");
////            }
////
////            if (oldScrollY - scrollY > px_10dp && t2 - t1 > 500) {
////            上滑
////                    System.out.println(stringBuilder + "up");
////            }
//            if (scrollY == 0) {
//                //顶部
//                System.out.println(stringBuilder + "top");
////                flagScroll = true;
//
//            }
//
//            if (scrollY == (v.getChildAt(0).getMeasuredHeight() - v.getMeasuredHeight())) {
//                //底部
//                System.out.println(stringBuilder + "bottom");
//                flagScroll = true;
//                beginPos = beginPos + 100 < FileListener.number ? beginPos : FileListener.number - 100;
//
//            }
//
//            if (!flagScroll) return;
////            if (v.fullScroll();)
//
////            startPos = currentPos - 250 > 0 ? currentPos - 250 : 0;
////            endPos = currentPos + 250 < FileListener.number ? currentPos + 250 : FileListener.number;
//
//            final PrecomputedTextCompat.Params params = textViewWeakReference.get().getTextMetricsParamsCompat();
//            FileListener.singleThread.execute(new Runnable() {
//
//                @Override
//                public void run() {
//                    int beginPosCopy = beginPos;
//                    StringBuilder stringBuilder = new StringBuilder(128 * 512);
//                    for (int i = 0; i < 100; i++) {
//                        stringBuilder.append(FileListener.FullInfo[beginPosCopy++]);
//                    }
//                    PrecomputedTextCompat precomputedText = PrecomputedTextCompat.create(stringBuilder, params);
//                    Message message = Message.obtain();
//                    message.what = 12;
//                    message.obj = precomputedText;
//                    FileListener.handler.sendMessage(message);
//                }
//            });
//            beginPos += 100;
//            flagScroll = false;
//        }
//    }
}
