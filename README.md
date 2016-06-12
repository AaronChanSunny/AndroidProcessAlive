# 关于 Android 进程保活

## 为什么需要做进程保活

作为一款即时通讯软件（IM），最基本的使命就是保证用户能够实时的收发消息。为了达到这个目的，就需要让我们的 IM 进程始终存活在后台中。那么，怎样才能让我们的 IM
进程始终存活而不会被系统干掉呢？这就需要了解 Android 的进程管理机制。默认情况下，Android
系统将尽可能长时间地保持应用进程，以达到应用之间切换的流畅度，但是如果系统资源吃紧，为了新建进程或运行更重要的进程，Android （其实就是 `Low Memory Killer`）会选择性地回收已有进程来释放内存。Low Memory Killer 对于进程的回收并不是随意的，它会将进程按照`重要程度`进行排序，当需要回收进程时，首先会考虑回收重要性最低的进程，然后是重要性略低的进程，以此类推回收系统资源。

### 进程优先级

- 前台进程

如果一个进程满足以下任一条件，则判定为前台进程：

1. 托管用户正在交互的 Activity（已调用 Activity#onResume() 方法）
2. 托管某个 Service，并且该 Service 绑定到用户正在交互的 Activity
3. 托管正在`前台`运行的 Service（服务已调用 startForeground()）
4. 托管正在执行某些生命周期回调的 Service（onCreate(), onStart(), onDestroy）
5. 托管正执行其 onReceive() 方法的 BroadcastReceiver

- 可见进程

没有任何前台组件，但仍然会影响用户在屏幕上所见内容的进程。如果一个进程满足以下任一条件，则可判定为可见进程：

1. 托管不在前台，但是仍然可见的 Activity（已调用 onPause() 方法）。例如，如果前台 Activity 启动了一个对话框，允许在其后显示上一
Activity，则有可能会发生这种情况
2. 托管绑定到可见（或前台）Activity 的 Service

- 服务进程

正在运行已使用 startService() 方法启动的服务且不属于上述两个更高类别进程的进程。

- 后台进程

包含目前对用户不可见的 Activity 的进程（已调用 Activity 的 onStop() 方法）。

- 空进程

不含任何活动应用组件的进程。

关于上述 5 个进程的优先级信息，在 `ActivityManager$RunningAppProcessInfo` 内部类中有相应的常量定义：

```
/**
 * Constant for {@link #importance}: This process is running the
 * foreground UI; that is, it is the thing currently at the top of the screen
 * that the user is interacting with.
 */
public static final int IMPORTANCE_FOREGROUND = 100;
/**
 * Constant for {@link #importance}: This process is running a foreground
 * service, for example to perform music playback even while the user is
 * not immediately in the app.  This generally indicates that the process
 * is doing something the user actively cares about.
 */
public static final int IMPORTANCE_FOREGROUND_SERVICE = 125;
/**
 * Constant for {@link #importance}: This process is running the foreground
 * UI, but the device is asleep so it is not visible to the user.  This means
 * the user is not really aware of the process, because they can not see or
 * interact with it, but it is quite important because it what they expect to
 * return to once unlocking the device.
 */
public static final int IMPORTANCE_TOP_SLEEPING = 150;
/**
 * Constant for {@link #importance}: This process is running something
 * that is actively visible to the user, though not in the immediate
 * foreground.  This may be running a window that is behind the current
 * foreground (so paused and with its state saved, not interacting with
 * the user, but visible to them to some degree); it may also be running
 * other services under the system's control that it inconsiders important.
 */
public static final int IMPORTANCE_VISIBLE = 200;
/**
 * Constant for {@link #importance}: This process is not something the user
 * is directly aware of, but is otherwise perceptable to them to some degree.
 */
public static final int IMPORTANCE_PERCEPTIBLE = 130;
/**
 * Constant for {@link #importance}: This process is running an
 * application that can not save its state, and thus can't be killed
 * while in the background.
 * @hide
 */
public static final int IMPORTANCE_CANT_SAVE_STATE = 170;
/**
 * Constant for {@link #importance}: This process is contains services
 * that should remain running.  These are background services apps have
 * started, not something the user is aware of, so they may be killed by
 * the system relatively freely (though it is generally desired that they
 * stay running as long as they want to).
 */
public static final int IMPORTANCE_SERVICE = 300;
/**
 * Constant for {@link #importance}: This process process contains
 * background code that is expendable.
 */
public static final int IMPORTANCE_BACKGROUND = 400;
/**
 * Constant for {@link #importance}: This process is empty of any
 * actively running code.
 */
public static final int IMPORTANCE_EMPTY = 500;
/**
 * Constant for {@link #importance}: This process does not exist.
 */
public static final int IMPORTANCE_GONE = 1000;
```

### 进程优先级之 `oom_adj`

`oom_adj` 是 Linux 内核分配给每个进程的一个值，它表示该进程的优先级，`Low Memory Killer` 对于进程的回收，最本质上就是根据这个值决定的。对于
`oom_adj`，我们需要了解以下两点：

- 进程的 oom_adj 越大，表示此进程优先级越低，越容易被杀回收；越小，表示进程优先级越高，越不容易被杀回收
- 普通 App 进程的 oom_adj >= 0,系统进程的 oom_adj 才可能 <0

`oom_adj` 值在 ProcessList 类中有具体的定义：

```
// OOM adjustments for processes in various states:
// Adjustment used in certain places where we don't know it yet.
// (Generally this is something that is going to be cached, but we
// don't know the exact value in the cached range to assign yet.)
static final int UNKNOWN_ADJ = 16;
// This is a process only hosting activities that are not visible,
// so it can be killed without any disruption.
static final int CACHED_APP_MAX_ADJ = 15;
static final int CACHED_APP_MIN_ADJ = 9;
// The B list of SERVICE_ADJ -- these are the old and decrepit
// services that aren't as shiny and interesting as the ones in the A
static final int SERVICE_B_ADJ = 8;
// This is the process of the previous application that the user was i
// This process is kept above other things, because it is very common
// switch back to the previous app.  This is important both for recent
// task switch (toggling between the two top recent apps) as well as n
// UI flow such as clicking on a URI in the e-mail app to view in the
// and then pressing back to return to e-mail.
static final int PREVIOUS_APP_ADJ = 7;
// This is a process holding the home application -- we want to try
// avoiding killing it, even if it would normally be in the background
// because the user interacts with it so much.
static final int HOME_APP_ADJ = 6;
// This is a process holding an application service -- killing it will
// have much of an impact as far as the user is concerned.
static final int SERVICE_ADJ = 5;
// This is a process with a heavy-weight application.  It is in the
// background, but we want to try to avoid killing it.  Value set in
// system/rootdir/init.rc on startup.
static final int HEAVY_WEIGHT_APP_ADJ = 4;
// This is a process currently hosting a backup operation.  Killing it
// is not entirely fatal but is generally a bad idea.
static final int BACKUP_APP_ADJ = 3;
// This is a process only hosting components that are perceptible to t
// user, and we really want to avoid killing them, but they are not
// immediately visible. An example is background music playback.
static final int PERCEPTIBLE_APP_ADJ = 2;
// This is a process only hosting activities that are visible to the
// user, so we'd prefer they don't disappear.
static final int VISIBLE_APP_ADJ = 1;
// This is the process running the current foreground app.  We'd reall
// rather not kill it!
static final int FOREGROUND_APP_ADJ = 0;
// This is a process that the system or a persistent process has bound
// and indicated it is important.
static final int PERSISTENT_SERVICE_ADJ = -11;
// This is a system persistent process, such as telephony.  Definitely
// don't want to kill it, but doing so is not completely fatal.
static final int PERSISTENT_PROC_ADJ = -12;
// The system process runs at the default adjustment.
static final int SYSTEM_ADJ = -16;
// Special code for native processes that are not being managed by the
// don't have an oom adj assigned by the system).
static final int NATIVE_ADJ = -17;
```

查看一个进程的 `oom_adj` 很简单：

```
cat /proc/进程ID/oom_adj
```

例如，要查看例子中所有 Service 的 `oom_adj` 值：

```
Administrator@ND--20160412ZPH MINGW64 ~
$ adb shell ps | grep me.aaron
u0_a60    13609 1140  1536592 45932 ffffffff 9d77da8a S me.aaron.androidprocessalive
u0_a60    13638 1140  1521060 29592 ffffffff 9d77da8a S me.aaron.androidprocessalive:normal
u0_a60    13652 1140  1522108 31136 ffffffff 9d77da8a S me.aaron.androidprocessalive:foreground
u0_a60    13668 1140  1523156 29720 ffffffff 9d77da8a S me.aaron.androidprocessalive:gray

Administrator@ND--20160412ZPH MINGW64 ~
$ adb shell cat /proc/13609/oom_adj
0

Administrator@ND--20160412ZPH MINGW64 ~
$ adb shell cat /proc/13638/oom_adj
7
```

## 保活思路

- 轻量化目标进程

通常情况下，应用的 UI 模块是系统资源占用大户。将一个应用的 UI 和核心组件进行拆分，可以大大降低传输层
Service 所属进程的系统资源开销，因此大大降低了被系统回收的可能性。例如 99U 将传输层组件单独运行在一个进程里（:coreService）。

- 提高进行的优先级

对于 Service 来说，就是以前台的方式启动Service。

- 双进程互相拉起

因为需要多开一个进程（不包含任何业务），不断进行轮询。方案待定。

## 保活手段

### 黑色保活

黑色保活是通过系统广播唤醒和外接 SDK 唤醒这两种方式实现的。在一些深度定制的第三方 Rom 和 Android N 上面，通过系统广播唤醒已经失效。SDK 唤醒，也称作全家桶唤醒，太破坏
Android 生态了，不推荐使用。

### 白色保活

启用前台 Service。常见的有音乐播放类 App 和 Shadowsocks。

### 灰色保活

所谓灰色保活，就是利用系统漏洞来启动一个前台的 Service，但是不会出现通知栏。这样，我们启动的前台 Service 就像后台 Service 一样运作，并且具有前台 Service
的优先级。当系统资源吃紧时，不容易称为回收对象，达到提高保活率的目的。

- API(<18)

当 API < 18，创建一个前台 Service 时需要隐藏通知栏很简单，只需要创建一个空的 Notification 即可。

```
startForeground(250, new Notification());
```

- API(>=18)

当 API >= 18 时，创建前台 Service 后，要隐藏掉通知栏需要一点技巧。大体思路是：启动目标前台 Service 的同时，再启动一个前台 Service（暂且称之为 InnerService），InnerService 和目标 Service 具有相同的 Notification Id。然后，在 InnerService#onStartCommand() 方法中，取消掉该通知栏，并且 Stop 掉自己。具体代码：

```
public class GrayService extends Service {

    private static final int GRAY_SERVICE_ID = -11111;

    public static void actionStart(Context context) {
        context.startService(new Intent(context, GrayService.class));
    }

    public GrayService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            InnerService.actionStart(this);
            startForeground(GRAY_SERVICE_ID, new Notification());
        } else {
            startForeground(GRAY_SERVICE_ID, new Notification());
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public static class InnerService extends Service {

        public static void actionStart(Context context) {
            context.startService(new Intent(context, InnerService.class));
        }

        public InnerService() {
        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            startForeground(GRAY_SERVICE_ID, new Notification());
            stopSelf();
            return super.onStartCommand(intent, flags, startId);
        }

        @Override
        public IBinder onBind(Intent intent) {
            // TODO: Return the communication channel to the service.
            throw new UnsupportedOperationException("Not yet implemented");
        }
    }

}
```

## 重新拉起

无论怎样轻量化目标进程，提高目标进程的优先级。对于一个的应用，除非它在系统的白名单中，否则总会有被 Android 系统回收的时候。既然被回收不可避免，如果我们能够在进程被干掉的时候再恰当时候再次拉起，也能做到曲线救国。

### Service 重启

通过在onStartCommand方法中返回不同的值可以告知系统让系统在Service因为资源吃紧被干掉后可以在资源不紧张时重启。默认情况下Service的返回值就是START_STICKY或START_STICKY_COMPATIBILITY。
不能依赖系统自带的 Service 重启机制，特别是在第三方定制的 Rom 上面会出现各种莫名其妙的失效。

### 进程守护

对于每一个目标进程，配对一个守护进程，让守护进程不断轮询检查目标进程是否存活，如果目标进程被回收了就将其拉起。

### Receiver 触发

开机启动、网络状态变化、充电状态变化等系统广播在高版本的系统和定制 Rom 上已经失效，这里不做讨论。

### 使用 AlarmManager 定时拉起进程

使用 AlarmManager 服务，定时拉起目标 Service。这里需要注意一点，需要对 Service 的状态进行判断。如果目标 Service
并没有被回收，不需要再次开启任务。这里以 GrayService 拉起 NotifyService 的场景进行说明（实际对应的就是保活进程 Service 拉起被系统干掉的 UI 进程的
Service），具体代码：

GrayService：
```
@Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            InnerService.actionStart(this);
            startForeground(GRAY_SERVICE_ID, new Notification());
        } else {
            startForeground(GRAY_SERVICE_ID, new Notification());
        }

        setupAlarm();

        return super.onStartCommand(intent, flags, startId);
    }

    private void setupAlarm() {
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent alarmIntent = new Intent(WakeReceiver.GRAY_WAKE_ACTION);
        PendingIntent operation = PendingIntent.getBroadcast(
                this,
                WAKE_REQUEST_CODE,
                alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        am.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis(),
                INTERVAL,
                operation);
    }
```

WakeReceiver：
```
public class WakeReceiver extends BroadcastReceiver {

    private static final String TAG = WakeReceiver.class.getSimpleName();
    public static final String GRAY_WAKE_ACTION = "me.aaron.androidprocessalive.receiver.WakeReceiver.GRAY_WAKE_ACTION";

    public WakeReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive");
        String action = intent.getAction();
        if (GRAY_WAKE_ACTION.equals(action)) {
            context.startService(new Intent(context, NotifyService.class));
        }
    }
}
```

NotifyService：
```
public class NotifyService extends Service {

    private static final String TAG = NotifyService.class.getSimpleName();

    private boolean mIsRunning;

    public static void actionStart(Context context) {
        context.startService(new Intent(context, NotifyService.class));
    }

    public NotifyService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");

        mIsRunning = false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");

        if (!mIsRunning) {
            mIsRunning = true;

            doSomething();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void doSomething() {
        Log.d(TAG, "doSomething...");

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    SystemClock.sleep(3000);
                    Log.d(TAG, "doing...");
                }
            }
        }).start();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }
}
```

## 参考

- [Processes and Threads](https://developer.android.com/guide/components/processes-and-threads.html)
- [论Android应用进程长存的可行性](http://blog.csdn.net/aigestudio/article/details/51348408)
- [微信Android客户端后台保活经验分享](http://www.infoq.com/cn/articles/wechat-android-background-keep-alive)
- [关于 Android 进程保活，你所需要知道的一切](http://www.jianshu.com/p/63aafe3c12af)