package zw.plugin.mock;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.ui.popup.Balloon;

/**
 * Created by zhengwei on 2017/12/3.
 */
public class EventLogger {

    private static final String GROUP_ID = "AutoMock";
    private static final String TITLE = "AutoMock Plugin Event Log";

    public static void log(String msg) {
        Notification notification = new Notification(GROUP_ID, TITLE, msg,
            NotificationType.INFORMATION);
        Notifications.Bus.notify(notification);
        Balloon balloon = notification.getBalloon();
        if (balloon != null) {
            balloon.hide(true);
        }
    }
}


