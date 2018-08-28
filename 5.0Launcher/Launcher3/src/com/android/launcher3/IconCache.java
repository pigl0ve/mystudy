/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Log;
import android.os.SystemProperties;

import com.android.launcher3.compat.LauncherActivityInfoCompat;
import com.android.launcher3.compat.LauncherAppsCompat;
import com.android.launcher3.compat.UserHandleCompat;
import com.android.launcher3.compat.UserManagerCompat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;

import com.mediatek.launcher3.ext.LauncherLog;

import android.util.Xml;
import com.android.internal.util.XmlUtils;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import java.util.HashMap;
import java.util.Iterator;
import com.android.launcher3.R;
import android.util.AttributeSet;
/*lvyanbing add for icon */
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.drawable.BitmapDrawable;
/*lvyanbing add for icon */
/**
 * Cache of application icons.  Icons can be made from any thread.
 */
public class IconCache {

    private static final String TAG = "Launcher.IconCache";

    private static final int INITIAL_ICON_CACHE_CAPACITY = 50;
    private static final String RESOURCE_FILE_PREFIX = "icon_";

    // Empty class name is used for storing package default entry.
    private static final String EMPTY_CLASS_NAME = ".";

    private static final boolean DEBUG = false;
    //lvyanbing add for app icon start
    private static final String TAG_FAVORITES = "applications";
    private static final String TAG_FAVORITE = "application";
    private final HashMap<String, ExtentalApp> extentalApps = new HashMap<String, IconCache.ExtentalApp>(INITIAL_ICON_CACHE_CAPACITY);
    /*lvyanbing add for icon */
    private Resources mResources;
    private float mScaleX;
    private float mScaleY;
    private float mRadius;
    /*lvyanbing add for icon */
    public  static class ExtentalApp {
    	public String packageName;
    	public String className;
    	public String title;
    	public Drawable icon;
    }
    //lvyanbing add for app icon end
    private static class CacheEntry {
        public Bitmap icon;
        public CharSequence title;
        public CharSequence contentDescription;
    }

    private static class CacheKey {
        public ComponentName componentName;
        public UserHandleCompat user;

        CacheKey(ComponentName componentName, UserHandleCompat user) {
            this.componentName = componentName;
            this.user = user;
        }

        @Override
        public int hashCode() {
            return componentName.hashCode() + user.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            CacheKey other = (CacheKey) o;
            return other.componentName.equals(componentName) && other.user.equals(user);
        }
    }

    private final HashMap<UserHandleCompat, Bitmap> mDefaultIcons =
            new HashMap<UserHandleCompat, Bitmap>();
    private final Context mContext;
    private final PackageManager mPackageManager;
    private final UserManagerCompat mUserManager;
    private final LauncherAppsCompat mLauncherApps;
    private final HashMap<CacheKey, CacheEntry> mCache =
            new HashMap<CacheKey, CacheEntry>(INITIAL_ICON_CACHE_CAPACITY);
    private int mIconDpi;

    public IconCache(Context context) {
        ActivityManager activityManager =
                (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        mContext = context;
        mPackageManager = context.getPackageManager();
        mUserManager = UserManagerCompat.getInstance(mContext);
        mLauncherApps = LauncherAppsCompat.getInstance(mContext);
        mIconDpi = activityManager.getLauncherLargeIconDensity();
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "IconCache, mIconDpi = " + mIconDpi);
        }

        // need to set mIconDpi before getting default icon
        UserHandleCompat myUser = UserHandleCompat.myUserHandle();
        mDefaultIcons.put(myUser, makeDefaultIcon(myUser));
        /*lvyanbing add for icon */
        mResources = mContext.getResources();
        mScaleX = mResources.getInteger(R.integer.hct_third_icon_scale_x) / 100.0F;
        mScaleY = mResources.getInteger(R.integer.hct_third_icon_scale_y) / 100.0F;
        mRadius = mResources.getInteger(R.integer.hct_third_icon_radius) / 100.0F;
        /*lvyanbing add for icon */
    }

    public Drawable getFullResDefaultActivityIcon() {
        return getFullResIcon(Resources.getSystem(),
                android.R.mipmap.sym_def_app_icon);
    }

    public Drawable getFullResIcon(Resources resources, int iconId) {
        Drawable d;
        try {
            d = resources.getDrawableForDensity(iconId, mIconDpi);
        } catch (Resources.NotFoundException e) {
            d = null;
        }

        return (d != null) ? d : getFullResDefaultActivityIcon();
    }

    public Drawable getFullResIcon(String packageName, int iconId) {
        Resources resources;
        try {
            resources = mPackageManager.getResourcesForApplication(packageName);
        } catch (PackageManager.NameNotFoundException e) {
            resources = null;
        }
        if (resources != null) {
            if (iconId != 0) {
                return getFullResIcon(resources, iconId);
            }
        }
        return getFullResDefaultActivityIcon();
    }

    public int getFullResIconDpi() {
        return mIconDpi;
    }

    public Drawable getFullResIcon(ResolveInfo info) {
        return getFullResIcon(info.activityInfo);
    }

    public Drawable getFullResIcon(ActivityInfo info) {

        Resources resources;
        try {
            resources = mPackageManager.getResourcesForApplication(
                    info.applicationInfo);
        } catch (PackageManager.NameNotFoundException e) {
            resources = null;
        }
        if (resources != null) {
            int iconId = info.getIconResource();
            if (iconId != 0) {
                return getFullResIcon(resources, iconId);
            }
        }

        return getFullResDefaultActivityIcon();
    }

    private Bitmap makeDefaultIcon(UserHandleCompat user) {
        Drawable unbadged = getFullResDefaultActivityIcon();
        Drawable d = mUserManager.getBadgedDrawableForUser(unbadged, user);
        Bitmap b = Bitmap.createBitmap(Math.max(d.getIntrinsicWidth(), 1),
                Math.max(d.getIntrinsicHeight(), 1),
                Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        d.setBounds(0, 0, b.getWidth(), b.getHeight());
        d.draw(c);
        c.setBitmap(null);
        return b;
    }

    /**
     * Remove any records for the supplied ComponentName.
     */
    public void remove(ComponentName componentName, UserHandleCompat user) {
        synchronized (mCache) {
            mCache.remove(new CacheKey(componentName, user));
        }
    }

    /**
     * Remove any records for the supplied package name.
     */
    public void remove(String packageName, UserHandleCompat user) {
        HashSet<CacheKey> forDeletion = new HashSet<CacheKey>();
        for (CacheKey key: mCache.keySet()) {
            if (key.componentName.getPackageName().equals(packageName)
                    && key.user.equals(user)) {
                forDeletion.add(key);
            }
        }
        for (CacheKey condemned: forDeletion) {
            mCache.remove(condemned);
        }
    }

    /**
     * Empty out the cache.
     */
    public void flush() {
        synchronized (mCache) {
            mCache.clear();

            /// M: Add for smart book feature. Need to update mIconDpi when plug in/out smart book.
            if (SystemProperties.get("ro.mtk_smartbook_support").equals("1")) {
                ActivityManager activityManager =
                        (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
                mIconDpi = activityManager.getLauncherLargeIconDensity();
                if (LauncherLog.DEBUG) {
                    LauncherLog.d(TAG, "flush, mIconDpi = " + mIconDpi);
                }
            }
        }

        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "Flush icon cache here.");
        }
    }

    /**
     * Empty out the cache that aren't of the correct grid size
     */
    public void flushInvalidIcons(DeviceProfile grid) {
        synchronized (mCache) {
            Iterator<Entry<CacheKey, CacheEntry>> it = mCache.entrySet().iterator();
            while (it.hasNext()) {
                final CacheEntry e = it.next().getValue();
                if ((e.icon != null) && (e.icon.getWidth() < grid.iconSizePx
                        || e.icon.getHeight() < grid.iconSizePx)) {
                    it.remove();
                }
            }
        }
    }

    /**
     * Fill in "application" with the icon and label for "info."
     */
    public void getTitleAndIcon(AppInfo application, LauncherActivityInfoCompat info,
            HashMap<Object, CharSequence> labelCache) {
        synchronized (mCache) {
            CacheEntry entry = cacheLocked(application.componentName, info, labelCache,
                    info.getUser(), false);

            application.title = entry.title;
            application.iconBitmap = entry.icon;
            application.contentDescription = entry.contentDescription;
        }
    }

    public Bitmap getIcon(Intent intent, UserHandleCompat user) {
        return getIcon(intent, null, user, true);
    }

    private Bitmap getIcon(Intent intent, String title, UserHandleCompat user, boolean usePkgIcon) {
        synchronized (mCache) {
            ComponentName component = intent.getComponent();
            // null info means not installed, but if we have a component from the intent then
            // we should still look in the cache for restored app icons.
            if (component == null) {
                return getDefaultIcon(user);
            }

            LauncherActivityInfoCompat launcherActInfo = mLauncherApps.resolveActivity(intent, user);
            CacheEntry entry = cacheLocked(component, launcherActInfo, null, user, usePkgIcon);
            if (title != null) {
                entry.title = title;
                entry.contentDescription = mUserManager.getBadgedLabelForUser(title, user);
            }
            return entry.icon;
        }
    }

    /**
     * Fill in "shortcutInfo" with the icon and label for "info."
     */
    public void getTitleAndIcon(ShortcutInfo shortcutInfo, Intent intent, UserHandleCompat user,
            boolean usePkgIcon) {
        synchronized (mCache) {
            ComponentName component = intent.getComponent();
            // null info means not installed, but if we have a component from the intent then
            // we should still look in the cache for restored app icons.
            if (component == null) {
                shortcutInfo.setIcon(getDefaultIcon(user));
                shortcutInfo.title = "";
                shortcutInfo.usingFallbackIcon = true;
            } else {
                LauncherActivityInfoCompat launcherActInfo =
                        mLauncherApps.resolveActivity(intent, user);
                CacheEntry entry = cacheLocked(component, launcherActInfo, null, user, usePkgIcon);

                shortcutInfo.setIcon(entry.icon);
                shortcutInfo.title = entry.title;
                shortcutInfo.usingFallbackIcon = isDefaultIcon(entry.icon, user);
            }
        }
    }


    public Bitmap getDefaultIcon(UserHandleCompat user) {
        if (!mDefaultIcons.containsKey(user)) {
            mDefaultIcons.put(user, makeDefaultIcon(user));
        }
        return mDefaultIcons.get(user);
    }

    public Bitmap getIcon(ComponentName component, LauncherActivityInfoCompat info,
            HashMap<Object, CharSequence> labelCache) {
        synchronized (mCache) {
            if (info == null || component == null) {
                return null;
            }

            CacheEntry entry = cacheLocked(component, info, labelCache, info.getUser(), false);
            return entry.icon;
        }
    }

    public boolean isDefaultIcon(Bitmap icon, UserHandleCompat user) {
        return mDefaultIcons.get(user) == icon;
    }

    private CacheEntry cacheLocked(ComponentName componentName, LauncherActivityInfoCompat info,
            HashMap<Object, CharSequence> labelCache, UserHandleCompat user, boolean usePackageIcon) {
        if (LauncherLog.DEBUG_LAYOUT) {
            LauncherLog.d(TAG, "cacheLocked: componentName = " + componentName
                    + ", info = " + info + ", HashMap<Object, CharSequence>:size = "
                    +  ((labelCache == null) ? "null" : labelCache.size()));
        }

        CacheKey cacheKey = new CacheKey(componentName, user);
        CacheEntry entry = mCache.get(cacheKey);
        ExtentalApp app  = extentalApps.get(componentName.getClassName());//lvyanbing add for app icon start
        Log.d(TAG, "cacheLocked: componentName = " + componentName );
        if (entry == null) {
            entry = new CacheEntry();

            mCache.put(cacheKey, entry);

            if (info != null) {
                ComponentName labelKey = info.getComponentName();
                if (labelCache != null && labelCache.containsKey(labelKey)) {
                    entry.title = labelCache.get(labelKey).toString();
                    if (LauncherLog.DEBUG_LOADERS) {
                        LauncherLog.d(TAG, "CacheLocked get title from cache: title = " + entry.title);
                    }
                } else {
                    entry.title = info.getLabel().toString();
                    if (LauncherLog.DEBUG_LOADERS) {
                        LauncherLog.d(TAG, "CacheLocked get title from pms: title = " + entry.title);
                    }
                    if (labelCache != null) {
                        labelCache.put(labelKey, entry.title);
                    }
                }

                entry.contentDescription = mUserManager.getBadgedLabelForUser(entry.title, user);
                entry.icon = Utilities.createIconBitmap(
                        info.getBadgedIcon(mIconDpi), mContext);
            } else {
                entry.title = "";
                Bitmap preloaded = getPreloadedIcon(componentName, user);
                if (preloaded != null) {
                    if (DEBUG) Log.d(TAG, "using preloaded icon for " +
                            componentName.toShortString());
                    entry.icon = preloaded;
                } else {
                    if (usePackageIcon) {
                        CacheEntry packageEntry = getEntryForPackage(
                                componentName.getPackageName(), user);
                        if (packageEntry != null) {
                            if (DEBUG) Log.d(TAG, "using package default icon for " +
                                    componentName.toShortString());
                            entry.icon = packageEntry.icon;
                            entry.title = packageEntry.title;
                        }
                    }
                    if (entry.icon == null) {
                        if (DEBUG) Log.d(TAG, "using default icon for " +
                                componentName.toShortString());
                        entry.icon = getDefaultIcon(user);
                    }
                }
            }
            /*lvyanbing add for icon */
	     if(!componentName.getClassName().equals("com.android.gallery3d.app.Gallery")
		  	&&!componentName.getClassName().equals("com.android.gallery3d.app.GalleryActivity")
		  	&&!componentName.getClassName().equals("com.android.settings.Settings")
			&&!componentName.getClassName().equals("com.android.dialer.DialtactsActivity")
			&&!componentName.getClassName().equals("com.android.contacts.activities.PeopleActivity")
			&&!componentName.getClassName().equals("com.android.mms.ui.BootActivity")
			&&!componentName.getClassName().equals("com.android.browser.BrowserActivity")
			&&!componentName.getClassName().equals("com.cn.bds.activity.LaunchActivity")
			&&!componentName.getClassName().equals("com.enjoy.bdmessage.EngineerTestBD")
			&&!componentName.getClassName().equals("com.enjoy.bdmessage.BDLocationActivity")
			&&!componentName.getClassName().equals("com.enjoy.bdmessage.MainActivity")
			&&!componentName.getClassName().equals("com.enjoy.bdmessage.ShowTimeActivity")
			&&!componentName.getClassName().equals("com.android.email.activity.Welcome")
			&&!componentName.getClassName().equals("com.android.calculator2.Calculator")
			&&!componentName.getClassName().equals("com.android.soundrecorder.SoundRecorder")
			&&!componentName.getClassName().equals("com.android.calendar.AllInOneActivity")
			&&!componentName.getClassName().equals("com.android.deskclock.DeskClock")
			&&!componentName.getClassName().equals("com.mediatek.fmradio.FmRadioActivity")
			&&!componentName.getClassName().equals("com.android.quicksearchbox.SearchActivity")
			&&!componentName.getClassName().equals("com.mediatek.filemanager.FileManagerOperationActivity")
			&&!componentName.getClassName().equals("com.android.providers.downloads.ui.DownloadList")
			&&!componentName.getClassName().equals("com.android.camera.CameraLauncher")
			&&!componentName.getClassName().equals("com.android.music.MusicBrowserActivity")
			&&!componentName.getClassName().equals("com.mediatek.blemanager.ui.BleLauncherActivity")
			&&!componentName.getClassName().equals("com.kmplayerpro.activity.googleAdVerIntro")
			&&!componentName.getClassName().equals("com.mediatek.datatransfer.MainActivity")
			&&!componentName.getClassName().equals("com.android.stk.StkMain")
			&&!componentName.getClassName().equals("com.android.sos_settings.SOS_Settings")
			&&!componentName.getClassName().equals("com.autonavi.map.activity.SplashActivity")
			&&!componentName.getClassName().equals("com.baidu.baidumaps.WelcomeScreen")
			&&!componentName.getClassName().equals("com.sohu.inputmethod.sogou.SogouIMELauncher")
		 	){
	        entry.icon = covertThirdIcon(entry.icon);
	     }
	     /*lvyanbing add for icon */
        }
        //lvyanbing add for app icon start
        if(app != null) {
            entry.icon =  Utilities.createIconBitmap(app.icon, mContext);
            if(app.title != null) {
                //entry.title = app.title;
            }
        }
        //lvyanbing add for app icon end
        return entry;
    }

    /**
     * Adds a default package entry in the cache. This entry is not persisted and will be removed
     * when the cache is flushed.
     */
    public void cachePackageInstallInfo(String packageName, UserHandleCompat user,
            Bitmap icon, CharSequence title) {
        remove(packageName, user);

        CacheEntry entry = getEntryForPackage(packageName, user);
        if (!TextUtils.isEmpty(title)) {
            entry.title = title;
        }
        if (icon != null) {
            entry.icon = Utilities.createIconBitmap(
                    new BitmapDrawable(mContext.getResources(), icon), mContext);
        }
    }

    /**
     * Gets an entry for the package, which can be used as a fallback entry for various components.
     */
    private CacheEntry getEntryForPackage(String packageName, UserHandleCompat user) {
        ComponentName cn = getPackageComponent(packageName);
        CacheKey cacheKey = new CacheKey(cn, user);
        CacheEntry entry = mCache.get(cacheKey);
        if (entry == null) {
            entry = new CacheEntry();
            entry.title = "";
            mCache.put(cacheKey, entry);

            try {
                ApplicationInfo info = mPackageManager.getApplicationInfo(packageName, 0);
                entry.title = info.loadLabel(mPackageManager);
                entry.icon = Utilities.createIconBitmap(info.loadIcon(mPackageManager), mContext);
            } catch (NameNotFoundException e) {
                if (DEBUG) Log.d(TAG, "Application not installed " + packageName);
            }

            if (entry.icon == null) {
                entry.icon = getPreloadedIcon(cn, user);
            }
        }
        return entry;
    }

    public HashMap<ComponentName,Bitmap> getAllIcons() {
        synchronized (mCache) {
            HashMap<ComponentName,Bitmap> set = new HashMap<ComponentName,Bitmap>();
            for (CacheKey ck : mCache.keySet()) {
                final CacheEntry e = mCache.get(ck);
                set.put(ck.componentName, e.icon);
            }
            return set;
        }
    }

    /**
     * Pre-load an icon into the persistent cache.
     *
     * <P>Queries for a component that does not exist in the package manager
     * will be answered by the persistent cache.
     *
     * @param context application context
     * @param componentName the icon should be returned for this component
     * @param icon the icon to be persisted
     * @param dpi the native density of the icon
     */
    public static void preloadIcon(Context context, ComponentName componentName, Bitmap icon,
            int dpi) {
        // TODO rescale to the correct native DPI
        try {
            PackageManager packageManager = context.getPackageManager();
            packageManager.getActivityIcon(componentName);
            // component is present on the system already, do nothing
            return;
        } catch (PackageManager.NameNotFoundException e) {
            // pass
        }

        final String key = componentName.flattenToString();
        FileOutputStream resourceFile = null;
        try {
            resourceFile = context.openFileOutput(getResourceFilename(componentName),
                    Context.MODE_PRIVATE);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            if (icon.compress(android.graphics.Bitmap.CompressFormat.PNG, 75, os)) {
                byte[] buffer = os.toByteArray();
                resourceFile.write(buffer, 0, buffer.length);
            } else {
                Log.w(TAG, "failed to encode cache for " + key);
                return;
            }
        } catch (FileNotFoundException e) {
            Log.w(TAG, "failed to pre-load cache for " + key, e);
        } catch (IOException e) {
            Log.w(TAG, "failed to pre-load cache for " + key, e);
        } finally {
            if (resourceFile != null) {
                try {
                    resourceFile.close();
                } catch (IOException e) {
                    Log.d(TAG, "failed to save restored icon for: " + key, e);
                }
            }
        }
    }

    /**
     * Read a pre-loaded icon from the persistent icon cache.
     *
     * @param componentName the component that should own the icon
     * @returns a bitmap if one is cached, or null.
     */
    private Bitmap getPreloadedIcon(ComponentName componentName, UserHandleCompat user) {
        final String key = componentName.flattenToShortString();

        // We don't keep icons for other profiles in persistent cache.
        if (!user.equals(UserHandleCompat.myUserHandle())) {
            return null;
        }

        if (DEBUG) Log.v(TAG, "looking for pre-load icon for " + key);
        Bitmap icon = null;
        FileInputStream resourceFile = null;
        try {
            resourceFile = mContext.openFileInput(getResourceFilename(componentName));
            byte[] buffer = new byte[1024];
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            int bytesRead = 0;
            while(bytesRead >= 0) {
                bytes.write(buffer, 0, bytesRead);
                bytesRead = resourceFile.read(buffer, 0, buffer.length);
            }
            if (DEBUG) Log.d(TAG, "read " + bytes.size());
            icon = BitmapFactory.decodeByteArray(bytes.toByteArray(), 0, bytes.size());
            if (icon == null) {
                Log.w(TAG, "failed to decode pre-load icon for " + key);
            }
        } catch (FileNotFoundException e) {
            if (DEBUG) Log.d(TAG, "there is no restored icon for: " + key);
        } catch (IOException e) {
            Log.w(TAG, "failed to read pre-load icon for: " + key, e);
        } finally {
            if(resourceFile != null) {
                try {
                    resourceFile.close();
                } catch (IOException e) {
                    Log.d(TAG, "failed to manage pre-load icon file: " + key, e);
                }
            }
        }

        if (icon != null) {
            // TODO: handle alpha mask in the view layer
            Bitmap b = Bitmap.createBitmap(Math.max(icon.getWidth(), 1),
                    Math.max(icon.getHeight(), 1),
                    Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(b);
            Paint paint = new Paint();
            paint.setAlpha(127);
            c.drawBitmap(icon, 0, 0, paint);
            c.setBitmap(null);
            icon.recycle();
            icon = b;
        }

        return icon;
    }

    /**
     * Remove a pre-loaded icon from the persistent icon cache.
     *
     * @param componentName the component that should own the icon
     * @returns true on success
     */
    public boolean deletePreloadedIcon(ComponentName componentName, UserHandleCompat user) {
        // We don't keep icons for other profiles in persistent cache.
        if (!user.equals(UserHandleCompat.myUserHandle())) {
            return false;
        }
        if (componentName == null) {
            return false;
        }
        if (mCache.remove(componentName) != null) {
            if (DEBUG) Log.d(TAG, "removed pre-loaded icon from the in-memory cache");
        }
        boolean success = mContext.deleteFile(getResourceFilename(componentName));
        if (DEBUG && success) Log.d(TAG, "removed pre-loaded icon from persistent cache");

        return success;
    }

    private static String getResourceFilename(ComponentName component) {
        String resourceName = component.flattenToShortString();
        String filename = resourceName.replace(File.separatorChar, '_');
        return RESOURCE_FILE_PREFIX + filename;
    }

    static ComponentName getPackageComponent(String packageName) {
        return new ComponentName(packageName, EMPTY_CLASS_NAME);
    }
    //lvyanbing add for app icon start
    public void loadExternalIconforXml() {
    	try {
			XmlResourceParser parser = mContext.getResources().getXml(R.xml.external_application);
			AttributeSet attrs = Xml.asAttributeSet(parser);
			XmlUtils.beginDocument(parser, TAG_FAVORITES);
			final int depth = parser.getDepth();
			int type;
			ExtentalApp extental = null;
			while (((type = parser.next()) != XmlPullParser.END_TAG || parser
					.getDepth() > depth)
					&& type != XmlPullParser.END_DOCUMENT) {

				if (type != XmlPullParser.START_TAG) {
					continue;
				}
				boolean added = false;
				final String name = parser.getName();
				TypedArray a = mContext.obtainStyledAttributes(attrs,
						R.styleable.ExternalApplication);
				extental = new ExtentalApp();
				extental.title = a.getString(R.styleable.ExternalApplication_ex_title);
				extental.icon = a.getDrawable(R.styleable.ExternalApplication_ex_icon);
				extental.packageName = a.getString(R.styleable.ExternalApplication_ex_packageName);
				extental.className = a.getString(R.styleable.ExternalApplication_ex_className);
				
				//LogUtils.debug(TAG, "extental.className====" + extental.className);
				if(TAG_FAVORITE.equals(name)) {
					extentalApps.put(extental.className, extental);
				}
				a.recycle();
			}
		} catch (XmlPullParserException e) {
			Log.w(TAG, "Got exception parsing extenal.", e);
			
		} catch (IOException e) {
			
			Log.w(TAG, "Got exception parsing extenal.", e);
		}
    }
    //lvyanbing add for app icon end
  /*lvyanbing add for icon */
  private Bitmap covertThirdIcon(Bitmap bitmapIcon)
  {
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inJustDecodeBounds = true;
    BitmapFactory.decodeResource(mResources, R.drawable.hct_apk_icon_bg_1, options);
    int i = options.outWidth;
    int j = options.outHeight;
    Drawable drawable = mResources.getDrawable(R.drawable.hct_apk_icon_bg_1);
    Bitmap bitmapBg = ((BitmapDrawable)drawable).getBitmap();
    return Utilities.createCompoundBitmap(bitmapBg, bitmapIcon, mScaleX, mScaleY, mRadius);
  }
  /*lvyanbing add for icon */
}
