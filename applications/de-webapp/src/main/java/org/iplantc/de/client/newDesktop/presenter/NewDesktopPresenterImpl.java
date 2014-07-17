package org.iplantc.de.client.newDesktop.presenter;

import static org.iplantc.de.commons.client.collaborators.presenter.ManageCollaboratorsPresenter.MODE.MANAGE;
import org.iplantc.de.client.DEClientConstants;
import org.iplantc.de.client.events.EventBus;
import org.iplantc.de.client.events.WindowCloseRequestEvent;
import org.iplantc.de.client.models.DEProperties;
import org.iplantc.de.client.models.HasPath;
import org.iplantc.de.client.models.UserInfo;
import org.iplantc.de.client.models.UserSettings;
import org.iplantc.de.client.models.WindowState;
import org.iplantc.de.client.models.notifications.NotificationCategory;
import org.iplantc.de.client.newDesktop.NewDesktopView;
import org.iplantc.de.client.periodic.MessagePoller;
import org.iplantc.de.client.preferences.views.PreferencesDialog;
import org.iplantc.de.client.services.UserSessionServiceFacade;
import org.iplantc.de.client.sysmsgs.view.NewMessageView;
import org.iplantc.de.client.util.CommonModelUtils;
import org.iplantc.de.client.views.windows.IPlantWindowInterface;
import org.iplantc.de.commons.client.CommonUiConstants;
import org.iplantc.de.commons.client.ErrorHandler;
import org.iplantc.de.commons.client.collaborators.views.ManageCollaboratorsDailog;
import org.iplantc.de.commons.client.requests.KeepaliveTimer;
import org.iplantc.de.commons.client.util.WindowUtil;
import org.iplantc.de.commons.client.views.gxt3.dialogs.IplantErrorDialog;
import org.iplantc.de.commons.client.views.window.configs.AppsWindowConfig;
import org.iplantc.de.commons.client.views.window.configs.ConfigFactory;
import org.iplantc.de.commons.client.views.window.configs.DiskResourceWindowConfig;
import org.iplantc.de.commons.client.views.window.configs.WindowConfig;
import org.iplantc.de.resources.client.messages.IplantErrorStrings;
import org.iplantc.de.shared.DeModule;
import org.iplantc.de.shared.services.PropertyServiceFacade;

import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.gwt.core.client.GWT;
import com.google.gwt.debug.client.DebugInfo;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.web.bindery.autobean.shared.impl.StringQuoter;

import com.sencha.gxt.core.client.util.KeyNav;
import com.sencha.gxt.widget.core.client.WindowManager;
import com.sencha.gxt.widget.core.client.event.DialogHideEvent;

import java.util.List;
import java.util.Map;

public class NewDesktopPresenterImpl implements NewDesktopView.Presenter {

    private static class BootstrapCallback implements AsyncCallback<String> {
        private final Provider<ErrorHandler> errorHandlerProvider;
        private final IplantErrorStrings errorStrings;
        private final UserInfo userInfo;

        public BootstrapCallback(UserInfo userInfo,
                                 Provider<ErrorHandler> errorHandlerProvider,
                                 IplantErrorStrings errorStrings) {

            this.userInfo = userInfo;
            this.errorHandlerProvider = errorHandlerProvider;
            this.errorStrings = errorStrings;
        }

        @Override
        public void onFailure(Throwable caught) {
            errorHandlerProvider.get().post(errorStrings.systemInitializationError(), caught);
        }

        @Override
        public void onSuccess(String result) {
            userInfo.init(result);
        }
    }

    private static class UserPreferencesCallback implements AsyncCallback<String> {
        private final Provider<ErrorHandler> errorHandlerProvider;
        private final IplantErrorStrings errorStrings;
        private final UserSettings userSettings;

        public UserPreferencesCallback(UserSettings userSettings,
                                       Provider<ErrorHandler> errorHandlerProvider,
                                       IplantErrorStrings errorStrings) {
            this.userSettings = userSettings;
            this.errorHandlerProvider = errorHandlerProvider;
            this.errorStrings = errorStrings;
        }

        @Override
        public void onFailure(Throwable caught) {
            errorHandlerProvider.get().post(errorStrings.systemInitializationError(), caught);
        }

        @Override
        public void onSuccess(String result) {
            userSettings.setValues(StringQuoter.split(result));
        }
    }

    private class LogoutCallback implements AsyncCallback<String> {
        private final DEClientConstants constants;
        private final UserSettings userSettings;

        private LogoutCallback(final DEClientConstants constants,
                               final UserSettings userSettings) {
            this.constants = constants;
            this.userSettings = userSettings;
        }

        @Override
        public void onFailure(Throwable arg0) {
            GWT.log("error on logout:" + arg0.getMessage());
            // logout anyway
            logout();
        }

        @Override
        public void onSuccess(String arg0) {
            GWT.log("logout service success:" + arg0);
            logout();
        }

        private void logout() {
            String redirectUrl = GWT.getHostPageBaseURL() + constants.logoutUrl();
            if (userSettings.isSaveSession()) {
                UserSessionProgressMessageBox uspmb = UserSessionProgressMessageBox.saveSession(NewDesktopPresenterImpl.this, redirectUrl);
                uspmb.show();
            } else {
                Window.Location.assign(redirectUrl);
            }
        }

    }

    class PropertyServiceCallback implements AsyncCallback<Map<String, String>> {
        private final DEProperties deProps;
        private final Provider<ErrorHandler> errorHandlerProvider;
        private final IplantErrorStrings errorStrings;
        private final Panel panel;
        private final UserInfo userInfo;
        private final Provider<UserSessionServiceFacade> userSessionServiceProvider;
        private final UserSettings userSettings;

        public PropertyServiceCallback(DEProperties deProperties,
                                       UserInfo userInfo,
                                       UserSettings userSettings,
                                       Provider<UserSessionServiceFacade> userSessionServiceProvider,
                                       Provider<ErrorHandler> errorHandlerProvider,
                                       IplantErrorStrings errorStrings,
                                       Panel panel) {
            this.deProps = deProperties;
            this.userInfo = userInfo;
            this.userSettings = userSettings;
            this.userSessionServiceProvider = userSessionServiceProvider;
            this.errorHandlerProvider = errorHandlerProvider;
            this.errorStrings = errorStrings;
            this.panel = panel;
        }

        @Override
        public void onFailure(Throwable caught) {
            errorHandlerProvider.get().post(errorStrings.systemInitializationError(), caught);
        }

        @Override
        public void onSuccess(Map<String, String> result) {
            deProps.initialize(result);
            final Request bootstrapReq = userSessionServiceProvider.get()
                                                                   .bootstrap(new BootstrapCallback(userInfo,
                                                                                                    errorHandlerProvider,
                                                                                                    errorStrings));
            final Request userPrefReq = userSessionServiceProvider.get()
                                                                  .getUserPreferences(new UserPreferencesCallback(userSettings,
                                                                                                                  errorHandlerProvider,
                                                                                                                  errorStrings));

            Timer t = new Timer() {
                @Override
                public void run() {
                    if (!bootstrapReq.isPending() && !userPrefReq.isPending()) {
                        // Cancel timer
                        cancel();
                        postBootstrap(panel);
                    }
                }
            };
            t.scheduleRepeating(1);
        }
    }

    interface AuthErrors {
        String API_NAME = "api_name";
        String ERROR = "error";
        String ERROR_DESCRIPTION = "error_description";
    }

    interface QueryStrings {
        String APP_CATEGORY = "app-category";
        String FOLDER = "folder";
        String TYPE = "type";
    }

    interface TypeQueryValues {
        String APPS = "apps";
        String DATA = "data";
    }

    @Inject CommonUiConstants commonUiConstants;
    @Inject DEClientConstants deClientConstants;
    @Inject IplantErrorStrings errorStrings;
    @Inject DEProperties deProperties;
    @Inject UserInfo userInfo;
    @Inject UserSettings userSettings;

    @Inject Provider<ErrorHandler> errorHandlerProvider;
    @Inject PropertyServiceFacade propertyServiceFacade;
    @Inject Provider<UserSessionServiceFacade> userSessionServiceProvider;

    private final EventBus eventBus;
    private final SaveSessionPeriodic ssp;
    private final NewMessageView.Presenter systemMsgPresenter;
    private final NewDesktopView view;
    private final DesktopWindowManager windowManager;

    @Inject
    public NewDesktopPresenterImpl(final NewDesktopView view,
                                   final DesktopPresenterEventHandler globalEventHandler,
                                   final DesktopPresenterWindowEventHandler windowEventHandler,
                                   final EventBus eventBus,
                                   final NewMessageView.Presenter systemMsgPresenter,
                                   final WindowManager gxtWindowManager) {
        this.view = view;
        this.eventBus = eventBus;
        this.systemMsgPresenter = systemMsgPresenter;
        this.view.setPresenter(this);
        globalEventHandler.setPresenter(this);
        windowEventHandler.setPresenter(this);
        windowManager = new DesktopWindowManager(gxtWindowManager);
        if (DebugInfo.isDebugIdEnabled()) {
            this.view.ensureDebugId(DeModule.Ids.DESKTOP);
        }
        ssp = new SaveSessionPeriodic(this);

    }

    public static native void doIntro() /*-{
        var introjs = $wnd.introJs();
        introjs.setOption("showStepNumbers", false);
        introjs.setOption("skipLabel", "Exit");
        introjs.start();
    }-*/;

    @Override
    public void doPeriodicSessionSave() {
        MessagePoller poller = MessagePoller.getInstance();
        if (userSettings.isSaveSession()) {

            ssp.run();
            poller.addTask(ssp);
            // start if not started...
            poller.start();
        } else {
            poller.removeTask(ssp);
        }
    }

    @Override
    public List<WindowState> getOrderedWindowStates() {
        List<WindowState> windowStates = Lists.newArrayList();
        for (Widget w : WindowManager.get().getStack()) {
            windowStates.add(((IPlantWindowInterface) w).getWindowState());
        }
        return windowStates;
    }

    @Override
    public void go(final Panel panel) {
        propertyServiceFacade.getProperties(new PropertyServiceCallback(deProperties,
                                                                        userInfo,
                                                                        userSettings,
                                                                        userSessionServiceProvider,
                                                                        errorHandlerProvider,
                                                                        errorStrings,
                                                                        panel));
    }

    @Override
    public void onAboutClick() {
        show(ConfigFactory.aboutWindowConfig());
    }

    @Override
    public void onAnalysesWinBtnSelect() {
        show(ConfigFactory.analysisWindowConfig());
    }

    @Override
    public void onAppsWinBtnSelect() {
        show(ConfigFactory.appsWindowConfig());
    }

    @Override
    public void onCollaboratorsClick() {
        ManageCollaboratorsDailog d = new ManageCollaboratorsDailog(MANAGE);
        d.show();
    }

    @Override
    public void onContactSupportClick() {
        WindowUtil.open(commonUiConstants.supportUrl());
    }

    @Override
    public void onDataWinBtnSelect() {
        show(ConfigFactory.diskResourceWindowConfig(true));
    }

    @Override
    public void onDocumentationClick() {
        WindowUtil.open(deClientConstants.deHelpFile());
    }

    @Override
    public void onForumsBtnSelect() {
        WindowUtil.open(commonUiConstants.forumsUrl());
    }

    @Override
    public void onIntroClick() {
        doIntro();
    }

    @Override
    public void onLogoutClick() {
        doLogout();
    }

    @Override
    public void onPreferencesClick() {
        PreferencesDialog d = new PreferencesDialog();
        d.show();
    }

    @Override
    public void onSystemMessagesClick() {
        show(ConfigFactory.systemMessagesWindowConfig(null));
    }

    @Override
    public void restoreWindows(List<WindowState> windowStates) {

    }

    @Override
    public void show(final WindowConfig config) {
        windowManager.show(config, false);
    }

    @Override
    public void show(final WindowConfig config, final boolean updateExistingWindow) {
        windowManager.show(config, updateExistingWindow);
    }

    void postBootstrap(final Panel panel) {
        setBrowserContextMenuEnabled(deProperties.isContextClickEnabled());
        // Initialize keepalive timer
        String target = deProperties.getKeepaliveTarget();
        int interval = deProperties.getKeepaliveInterval();
        if (target != null && !target.equals("") && interval > 0) {
            KeepaliveTimer.getInstance().start(target, interval);
        }


        initMessagePoller();
        setUpKBShortCuts();
        panel.add(view);
        processQueryStrings();
    }

    private void doLogout() {
        // Need to stop polling
        MessagePoller.getInstance().stop();
//        cleanUp();

        userSessionServiceProvider.get().logout(new LogoutCallback(deClientConstants,
                                                                   userSettings));
    }

    private void getUserSession() {
        boolean urlHasDataTypeParameter = urlHasDataTypeParameter(Window.Location.getParameterMap());
        if (userSettings.isSaveSession() && !urlHasDataTypeParameter) {
            // This restoreSession's callback will also init periodic session saving.
            UserSessionProgressMessageBox uspmb = UserSessionProgressMessageBox.restoreSession(this);
            uspmb.show();
        } else if (urlHasDataTypeParameter) {
            doPeriodicSessionSave();
        }
    }

    /**
     * TODO JDS This needs to be ported to notifications.
     */
    private void initMessagePoller() {
        // Do an initial fetch of message counts, otherwise the initial count will not be fetched until
        // after an entire poll-length of the MessagePoller's timer (15 seconds by default).
        GetMessageCounts notificationCounts = new GetMessageCounts();
        notificationCounts.run();
        MessagePoller poller = MessagePoller.getInstance();
        poller.addTask(notificationCounts);
        poller.start();
    }

    private void processQueryStrings() {
        boolean hasError = false;
        Map<String, List<String>> params = Window.Location.getParameterMap();
        for (String key : params.keySet()) {

            if (QueryStrings.TYPE.equalsIgnoreCase(key)) { // Process query strings for opening DE windows
                for (String paramValue : params.get(key)) {
                    WindowConfig windowConfig = null;

                    if (TypeQueryValues.APPS.equalsIgnoreCase(paramValue)) {
                        final AppsWindowConfig appsConfig = ConfigFactory.appsWindowConfig();
                        final String appCategoryId = Window.Location.getParameter(QueryStrings.APP_CATEGORY);
                        appsConfig.setSelectedAppGroup(CommonModelUtils.createHasIdFromString(appCategoryId));
                        windowConfig = appsConfig;
                    } else if (TypeQueryValues.DATA.equalsIgnoreCase(paramValue)) {
                        DiskResourceWindowConfig drConfig = ConfigFactory.diskResourceWindowConfig(true);
                        drConfig.setMaximized(true);
                        // If user has multiple folder parameters, the last one will be used.
                        String folderParameter = Window.Location.getParameter(QueryStrings.FOLDER);
                        String selectedFolder = URL.decode(Strings.nullToEmpty(folderParameter));

                        if (!Strings.isNullOrEmpty(selectedFolder)) {
                            HasPath folder = CommonModelUtils.createHasPathFromString(selectedFolder);
                            drConfig.setSelectedFolder(folder);
                        }
                        windowConfig = drConfig;
                    }

                    if (windowConfig != null) {
                        show(windowConfig);
                    }

                }
            } else if (AuthErrors.ERROR.equalsIgnoreCase(key)) { // Process errors
                hasError = true;
                // Remove underscores, and upper case whole error
                String upperCaseError = Iterables.getFirst(params.get(key), "").replaceAll("_", " ").toUpperCase();
                String apiName = Strings.nullToEmpty(Window.Location.getParameter(AuthErrors.API_NAME));
                String titleApi = apiName.isEmpty() ? "" : " : " + apiName;
                IplantErrorDialog errorDialog = new IplantErrorDialog(upperCaseError + titleApi,
                                                                      Window.Location.getParameter(AuthErrors.ERROR_DESCRIPTION));
                errorDialog.show();
                errorDialog.addDialogHideHandler(new DialogHideEvent.DialogHideHandler() {
                    @Override
                    public void onDialogHide(DialogHideEvent event) {
                        getUserSession();
                    }
                });
            }
        }
        if (!hasError) getUserSession();
    }

    /**
     * Disable the context menu of the browser using native JavaScript.
     * <p/>
     * This disables the user's ability to right-click on this widget and get the browser's context menu
     */
    private native void setBrowserContextMenuEnabled(boolean enabled)
    /*-{
        $doc.oncontextmenu = function () {
            return enabled;
        };
    }-*/;

    private void setUpKBShortCuts() {
        new KeyNav(RootPanel.get()) {
            @Override
            public void handleEvent(NativeEvent event) {
                if (event.getCtrlKey() && event.getShiftKey()) {
                    final String keycode = String.valueOf((char) event.getKeyCode());
                    if (userSettings.getDataShortCut().equals(keycode)) {
                        show(ConfigFactory.diskResourceWindowConfig(true));
                    } else if (userSettings.getAnalysesShortCut().equals(keycode)) {
                        show(ConfigFactory.analysisWindowConfig());
                    } else if (userSettings.getAppsShortCut().equals(keycode)) {
                        show(ConfigFactory.appsWindowConfig());
                    } else if (userSettings.getNotifiShortCut().equals(keycode)) {
                        show(ConfigFactory.notifyWindowConfig(NotificationCategory.ALL));
                    } else if (userSettings.getCloseShortCut().equals(keycode)) {
                        eventBus.fireEvent(new WindowCloseRequestEvent());
                    }
                }
            }
        };
    }

    /**
     * @param params the url parameters map
     * @return true if parameter has "type=data"
     */
    private boolean urlHasDataTypeParameter(Map<String, List<String>> params) {
        for (String key : params.keySet()) {
            if (QueryStrings.TYPE.equalsIgnoreCase(key)
                    && TypeQueryValues.DATA.equalsIgnoreCase(Iterables.getFirst(params.get(key), ""))) {
                return true;
            }
        }
        return false;
    }

}
