package com.odin2.ds4bridgehelper;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.*;
import android.os.Bundle;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class PickerActivity extends Activity {
    private static final String TARGETS_FILE = "targets.conf";
    private final List<Choice> choices = new ArrayList<>(), visible = new ArrayList<>();
    private final Set<String> selected = new HashSet<>();
    private final Map<String, Drawable> icons = new HashMap<>();
    private PackageManager pm;
    private BaseAdapter adapter;
    private TextView status;
    private EditText search;
    private int foreground, secondary, background, accent;
    private int appliedTheme;
    private SettingsUi ui;
    private ListView list;
    private Button saveButton;
    private PopupMenu optionsPopup;

    private static final class Choice {
        final String pkg, label;
        final boolean system;
        final ApplicationInfo app;
        Choice(String pkg, String label, boolean system, ApplicationInfo app) {
            this.pkg = pkg; this.label = label; this.system = system; this.app = app;
        }
    }

    @Override protected void onCreate(Bundle state) {
        int mode = getPreferences(MODE_PRIVATE).getInt("theme", 0);
        appliedTheme=mode;
        boolean dark = mode == 2 || (mode == 0 && (getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES);
        setTheme(dark ? android.R.style.Theme_Material_NoActionBar
                : android.R.style.Theme_Material_Light_NoActionBar);
        super.onCreate(state);
        ui=new SettingsUi(this);foreground=ui.foreground;secondary=ui.secondary;background=ui.background;accent=ui.accent;
        getWindow().setStatusBarColor(background);
        getWindow().setNavigationBarColor(background);
        getWindow().getDecorView().setSystemUiVisibility(dark ? 0 :
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        pm = getPackageManager();
        ArrayList<String> pending = state == null ? null : state.getStringArrayList("selected");
        if (pending == null) selected.addAll(readTargets());
        else selected.addAll(pending);
        buildScreen();
        loadChoices();
        if (state != null) search.setText(state.getString("query", ""));
        filterChoices();
    }

    @Override protected void onSaveInstanceState(Bundle outState) {
        outState.putStringArrayList("selected", new ArrayList<>(selected));
        outState.putString("query", search.getText().toString());
        super.onSaveInstanceState(outState);
    }
    @Override protected void onResume(){super.onResume();if(appliedTheme!=getPreferences(MODE_PRIVATE).getInt("theme",0)){recreate();return;}filterChoices();}

    private void buildScreen() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(background);
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(20), 0, dp(16), 0);header.setBackgroundColor(ui.panel);
        TextView title = ui.heading("DS4 Bridge");title.setTextSize(20);
        title.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(title, new LinearLayout.LayoutParams(-1, dp(48)));
        page.addView(header);
        LinearLayout content=ui.column();content.setPadding(dp(16),dp(8),dp(16),dp(8));page.addView(content,new LinearLayout.LayoutParams(-1,0,1));
        TextView section=ui.heading("App selection"),help=ui.note("Choose the installed apps that should receive DS4 controller input.");
        if(getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE){LinearLayout intro=new LinearLayout(this);intro.setGravity(Gravity.CENTER_VERTICAL);intro.addView(section,new LinearLayout.LayoutParams(dp(190),-2));intro.addView(help,new LinearLayout.LayoutParams(0,-2,1));content.addView(intro);}else{content.addView(section);content.addView(help);}

        LinearLayout controls = new LinearLayout(this);
        controls.setGravity(Gravity.CENTER_VERTICAL);
        controls.setPadding(0,dp(4),0,dp(4));
        search = new EditText(this);
        search.setSingleLine(true); search.setHint("Search apps or package IDs");
        search.setTextColor(foreground); search.setHintTextColor(secondary);
        search.setTextSize(15);search.setBackground(ui.background(false));search.setPadding(dp(12),dp(8),dp(12),dp(8));
        Drawable searchIcon=getDrawable(android.R.drawable.ic_menu_search).mutate();searchIcon.setTint(secondary);searchIcon.setBounds(0,0,dp(20),dp(20));search.setCompoundDrawables(searchIcon,null,null,null);search.setCompoundDrawablePadding(dp(8));
        controls.addView(search, new LinearLayout.LayoutParams(0, dp(44), 1));
        TextView more = new TextView(this);
        more.setText("\u22ee");
        more.setTextSize(28);
        more.setTextColor(foreground);
        more.setGravity(Gravity.CENTER);
        more.setContentDescription("More options");
        more.setFocusable(true);
        more.setOnClickListener(v -> showOptions(more));
        controls.addView(more, new LinearLayout.LayoutParams(dp(48), dp(44)));
        content.addView(controls);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filterChoices(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        status = new TextView(this);
        status.setTextColor(secondary); status.setTextSize(13);
        status.setPadding(0,dp(4),0,dp(8));
        content.addView(status);

        list = new ListView(this);
        list.setDivider(null);list.setDividerHeight(0);list.setCacheColorHint(background);list.setBackgroundColor(background);list.setSelector(android.R.color.transparent);
        adapter = new BaseAdapter() {
            @Override public int getCount() { return visible.size(); }
            @Override public Object getItem(int position) { return visible.get(position); }
            @Override public long getItemId(int position) { return position; }
            @Override public View getView(int position, View convertView, ViewGroup parent) {
                return makeRow(visible.get(position));
            }
        };
        list.setAdapter(adapter);
        list.setOnItemClickListener((parent, view, position, id) -> {
            String pkg = visible.get(position).pkg;
            if (!selected.add(pkg)) selected.remove(pkg);
            adapter.notifyDataSetChanged(); updateStatus();
        });
        content.addView(list, new LinearLayout.LayoutParams(-1, 0, 1));
        saveButton=ui.button("Save selected apps",true);saveButton.setOnClickListener(v -> saveTargets());
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(-1, dp(48));saveParams.setMargins(0,dp(4),0,0);content.addView(saveButton,saveParams);
        setContentView(page);
    }

    private View makeRow(Choice choice) {
        FrameLayout container=new FrameLayout(this);container.setPadding(0,0,0,dp(6));container.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(8), dp(14), dp(8));
        row.setBackground(ui.background(selected.contains(choice.pkg)));row.setMinimumHeight(dp(64));container.addView(row,new FrameLayout.LayoutParams(-1,-2));
        ImageView icon = new ImageView(this);
        Drawable drawable = icons.get(choice.pkg);
        if (drawable == null && choice.app != null) {
            drawable = choice.app.loadIcon(pm); icons.put(choice.pkg, drawable);
        }
        icon.setImageDrawable(drawable != null ? drawable : pm.getDefaultActivityIcon());
        row.addView(icon, new LinearLayout.LayoutParams(dp(40), dp(40)));
        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0, -2, 1);
        labelParams.setMargins(dp(16), 0, dp(8), 0);
        TextView name = new TextView(this);
        name.setText(choice.label); name.setTextSize(17); name.setTextColor(foreground);
        name.setSingleLine(true); name.setEllipsize(TextUtils.TruncateAt.END);
        labels.addView(name);
        TextView pkg = new TextView(this);
        pkg.setText(choice.pkg); pkg.setTextSize(12); pkg.setTextColor(secondary);
        pkg.setSingleLine(true); pkg.setEllipsize(TextUtils.TruncateAt.END);
        labels.addView(pkg);
        row.addView(labels, labelParams);
        CheckBox check = new CheckBox(this);
        check.setButtonTintList(new android.content.res.ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{accent,secondary}));
        check.setChecked(selected.contains(choice.pkg));
        check.setContentDescription("Select " + choice.label);
        check.setOnClickListener(v -> {
            if (check.isChecked()) selected.add(choice.pkg); else selected.remove(choice.pkg);
            adapter.notifyDataSetChanged(); updateStatus();
        });
        row.addView(check);
        container.setContentDescription(choice.label + ", " + choice.pkg
                + (selected.contains(choice.pkg) ? ", selected" : ", not selected"));
        return container;
    }

    private void showOptions(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        optionsPopup=popup;
        Menu menu = popup.getMenu();
        MenuItem showSystem=menu.add(0,1,0,"Show system apps");showSystem.setCheckable(true);showSystem.setChecked(getPreferences(MODE_PRIVATE).getBoolean("show_system",false));
        MenuItem startAtBoot = menu.add(0, 5, 1, "Start bridge after reboot");
        startAtBoot.setCheckable(true);
        startAtBoot.setChecked(BootStarter.isEnabled(this));
        menu.add(0,6,2,"Settings");
        popup.setOnMenuItemClickListener(item -> {
            if(item.getItemId()==1){boolean show=!getPreferences(MODE_PRIVATE).getBoolean("show_system",false);getPreferences(MODE_PRIVATE).edit().putBoolean("show_system",show).apply();item.setChecked(show);filterChoices();}
            else if (item.getItemId() == 5) {
                boolean enabled = !BootStarter.isEnabled(this);
                BootStarter.setEnabled(this, enabled);
                Toast.makeText(this, enabled ? "Bridge will start after the next reboot"
                        : "Automatic startup is off", Toast.LENGTH_LONG).show();
            } else if (item.getItemId()==6)startActivity(new Intent(this,BridgeActivity.class));
            return true;
        });
        popup.show();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
    private File targetFile() {
        Context storage = createDeviceProtectedStorageContext();
        return new File(storage.getFilesDir(), TARGETS_FILE);
    }
    private Set<String> readTargets() {
        Set<String> targets = new HashSet<>();
        File file = targetFile();
        if (!file.exists()) return targets;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) if (validPackage(line)) targets.add(line);
        } catch (Exception e) {
            Toast.makeText(this, "Could not read saved apps", Toast.LENGTH_LONG).show();
        }
        return targets;
    }
    private static boolean validPackage(String pkg) {
        return pkg.matches("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)+");
    }
    private void loadChoices() {
        Set<String> included = new HashSet<>();
        for (ApplicationInfo app : pm.getInstalledApplications(0)) {
            String pkg = app.packageName;
            if (!included.add(pkg) || pkg.equals(getPackageName())) continue;
            boolean system = (app.flags & (ApplicationInfo.FLAG_SYSTEM
                    | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
            choices.add(new Choice(pkg, String.valueOf(app.loadLabel(pm)), system, app));
        }
        selected.retainAll(included);
        selected.remove(getPackageName());
        Collections.sort(choices, Comparator.comparing(c -> c.label.toLowerCase(Locale.ROOT)));
    }
    private void filterChoices() {
        if (adapter == null || search == null) return;
        String needle = search.getText().toString().toLowerCase(Locale.ROOT);
        boolean showSystem = getPreferences(MODE_PRIVATE).getBoolean("show_system", false);
        visible.clear();
        for (Choice c : choices) {
            if (!showSystem && c.system) continue;
            if (c.label.toLowerCase(Locale.ROOT).contains(needle)
                    || c.pkg.toLowerCase(Locale.ROOT).contains(needle)) visible.add(c);
        }
        adapter.notifyDataSetChanged(); updateStatus();
    }
    private void updateStatus() {
        if (visible.isEmpty() && search.getText().length() > 0) {
            status.setText("No matching apps | " + selected.size() + " selected");
        } else if (visible.isEmpty()
                && !getPreferences(MODE_PRIVATE).getBoolean("show_system", false)) {
            status.setText("No user apps installed | Open options > Show system apps");
        } else {
            status.setText(visible.size() + (visible.size()==1?" app shown | ":" apps shown | ") + selected.size() + " selected");
        }
    }
    private void saveTargets() {
        List<String> sorted = new ArrayList<>(selected);
        Collections.sort(sorted);
        File target = targetFile();
        File temporary = new File(target.getParentFile(), TARGETS_FILE + ".tmp");
        try (FileOutputStream stream = new FileOutputStream(temporary);
             OutputStreamWriter writer = new OutputStreamWriter(stream, StandardCharsets.UTF_8)) {
            for (String pkg : sorted) if (validPackage(pkg)) writer.write(pkg + "\n");
            writer.flush(); stream.getFD().sync();
        } catch (Exception e) { status.setText("Could not save apps: " + e.getMessage()); return; }
        if (!temporary.renameTo(target)) { status.setText("Could not replace the saved app list."); return; }
        Toast.makeText(this, sorted.isEmpty() ? "Forwarding is off."
                : "Saved " + sorted.size() + " apps.", Toast.LENGTH_SHORT).show();
        updateStatus();
    }
}
