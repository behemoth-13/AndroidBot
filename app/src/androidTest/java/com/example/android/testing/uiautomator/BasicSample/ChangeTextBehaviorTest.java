package com.example.android.testing.uiautomator.BasicSample;

import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Environment;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SdkSuppress;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.Direction;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject2;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.Until;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 18)
public class ChangeTextBehaviorTest {

    private static final String PACKAGE = "net.okitoo.hackers";
    private static final String MY_IP = "53.95.120.218";
    private static final int LAUNCH_TIMEOUT = 5000;
    private static final String PATH_SAVED_CLIENTS = "Download/MyBot/SavedClients.txt";
    private static final String PATH_SAVED_MISSIONS = "Download/MyBot/SavedMissions.txt";
    private static final float MAX_REP = 1.5f;//1.5 default
    private static final float MIN_REP = 0.75f;//0.75 default
    private static  boolean IS_LAUNCHED = true;

    private UiDevice mDevice;
    private Queue<Client> clients = new LinkedList<>();
    private Map<String, Date> missions = new HashMap<>();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
    private int myRep = 0;

//закомментированы 66,
//добавлено 438         collectMissions();
    @Test
    public void runTest() throws UiObjectNotFoundException {
        //proxyMethod();
        runBot();
    }

    private void proxyMethod() {
        try {
            runBot();
        } catch (Exception e) {
            proxyMethod();
        }
    }

    private void runBot() throws UiObjectNotFoundException {
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        if(!IS_LAUNCHED) {
            mDevice.pressHome();
            final Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            PackageManager pm = InstrumentationRegistry.getContext().getPackageManager();
            ResolveInfo resolveInfo = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
            final String launcherPackage = resolveInfo.activityInfo.packageName;
            assertThat(launcherPackage, notNullValue());
            mDevice.wait(Until.hasObject(By.pkg(launcherPackage).depth(0)), LAUNCH_TIMEOUT);

            Context context = InstrumentationRegistry.getContext();
            final Intent intent1 = context.getPackageManager()
                    .getLaunchIntentForPackage(PACKAGE);
            intent1.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);    // Clear out any previous instances
            context.startActivity(intent1);

            // Wait for the app to appear
            mDevice.wait(Until.hasObject(By.pkg(PACKAGE).depth(0)), LAUNCH_TIMEOUT);
            mDevice.wait(Until.findObject(By.res(PACKAGE, "whois_done")), 120000);
            mDevice.findObject(By.res(PACKAGE, "whois_done")).click();
        }

        initSavedIP();
        initSavedMissions();
        waitAndClick("img_connection");                            //диспетчер подключений
        myRep = Integer.parseInt(waitAndGetText("stat_rep"));//берем репутацию
        waitAndClick("connection_firewall");                //журнал подключений
        while(waitAndIsExist("btn_close2","log_item_ip")){  //закрыть, ip в журнале подключений
            String ip = waitAndGetText("log_item_ip");      //ip в журнале подключений
            saveIP(ip);
            click("log_item_del");                          //удалить в журнале подключений
            sleep(1000);
        }
        click("btn_close2");                                //закрыть, ip в журнале подключений
        sleep(500);
        while(mDevice.hasObject(By.res(PACKAGE, "list_ip"))) {
            saveIP(waitAndGetText("list_ip"));
            int x = 1000;
            int y = 60;
            mDevice.swipe(x, y, x, y, 80);
            sleep(2000);
        }
        writeClientsToDisc();

        //while true
        while (true) {
            Client nextClient = getNextClient();

            waitObj("connection_new_target");
            mDevice.findObject(By.res(PACKAGE, "connection_new_target")).setText("");
            mDevice.findObject(By.res(PACKAGE, "connection_new_target")).setText(nextClient.getIp());//вводим ip
            click("connections_add");
            sleep(1000);//try to delete


            while (!mDevice.hasObject(By.res("android", "button1")) && !mDevice.hasObject(By.res(PACKAGE, "whois"))) {
                sleep(500);
            }
            if (mDevice.hasObject(By.res("android", "button1"))) {
                if(mDevice.hasObject(By.res(PACKAGE, "app_input_prompt"))) {//капча
                    Log.w("MyTag" , "captcha");
                    String s = waitAndGetText("app_input_prompt");
                    Pattern p = Pattern.compile("!:\\s[0-9]{3}");
                    Matcher m = p.matcher(s);
                    m.find();
                    mDevice.findObject(By.res(PACKAGE, "app_input_data")).setText(s.substring(m.start() + 3, m.end()));//ввод капчи
                    mDevice.findObject(By.res("android", "button1")).click();
                    waitAndClick("whois_done");
                    waitAndClick("connections_add");
                    sleep(1000);//try to delete
                }
            }

            String s = waitAndGetText("whois");
            while (!s.contains("Репутация:")) {
                sleep(200);
                s = waitAndGetText("whois");
                if (s.contains("Invalid IP")) {
                    break;
                }
            }
            if (clientIsValid(nextClient, s)) {
                waitAndClick("whois_add_to_list");//добавить ip в список подключений справа
                sleep(1000);
                waitAndClick("bounce_add");//добавить ip в список подключений слева
                sleep(500);
                waitAndClick("connections_connect");//подключиться
                while (!mDevice.hasObject(By.res("android", "button1")) && !mDevice.hasObject(By.res(PACKAGE, "software_i_icon"))) {
                    sleep(200);
                }

                if (mDevice.hasObject(By.res("android", "button1"))) { //если мой IP в черном списке
                    if (!mDevice.findObject(By.res("android", "alertTitle")).getText().equals("Connecting...")) {
                        mDevice.findObject(By.res("android", "button1")).click();
                        waitAndClick("bounce_delete");//закрыть ip слева
                        sleep(500);
                        int x = 1000;
                        int y = 60;
                        mDevice.swipe(x, y, x, y, 80);//закрыть ip в списке справа
                        sleep(500);
                        continue;
                    }
                }
                waitObj("software_i_icon");
                sleep(200);
                mDevice.findObjects(By.res(PACKAGE, "software_i_icon")).get(1).click();//взломать
                sleep(1000);
                boolean isCollect = false;
                boolean logIsDeleted = false;
                boolean ipIsSaved = false;
                int rests = getRestSeconds();
                while(rests > 8) {//отслеживание
                    if ((nextClient.getAction() != null) && (nextClient.getAction().equals(Client.DELETE))) {//delete mission
                        if (!logIsDeleted) {
                            while (!mDevice.hasObject(By.res(PACKAGE, "app_log")) &&
                                    mDevice.hasObject(By.res(PACKAGE, "ct_login_name"))) {
                                sleep(100);
                            }
                            try {
                                mDevice.wait(Until.findObject(By.res(PACKAGE, "app_log")), 1000);
                                click("app_log");//Лог
                            } catch (NullPointerException e) {
                                Log.w("MyTag", "NPE");
                                break;
                            }
                            mDevice.wait(Until.findObject(By.res(PACKAGE, "log_item_ip")), 1000);//ip в логах
                            List<UiObject2> logs = mDevice.findObjects(By.res(PACKAGE, "log_item_ip"));//ip в логах
                            int countLogs = logs.size();
                            boolean missionComplete = false;
                            for (int i1 = 0; i1 < countLogs; i1++) {
                                UiObject2 log = logs.get(i1);
                                if (log.getText().equals(MY_IP)) {//my ip
                                    log.getParent().findObject(By.res(PACKAGE, "log_item_del")).click(); //удалить лог
                                    i1--;
                                    sleep(1000);
                                } else if (!missionComplete) {
                                    log.getParent().findObject(By.res(PACKAGE, "log_item_del")).click(); //удалить лог
                                    i1--;
                                    missionComplete = true;
                                    nextClient.setAction(Client.NOTHING);
                                    sleep(1000);
                                }
                                waitObj("btn_disconnect");//кнопка отключения не видна изза всплывающего окна
                                logs = mDevice.findObjects(By.res(PACKAGE, "log_item_ip"));//ip в логах
                                countLogs = logs.size();
                                rests = getRestSeconds();
                                if (rests < 7) {
                                    break;
                                }
                            }
                            logIsDeleted = true;
                            rests = getRestSeconds();
                        } else if (!ipIsSaved){
                            //for (int i2 = 0; i2 < 1; i2++) {
                            List<UiObject2> logs = mDevice.findObjects(By.res(PACKAGE, "log_item_ip"));//ip в логах
//                            UiObject2 lastLog = null;
                            for (UiObject2 log : logs) {
                                saveIP(log.getText());
                                //lastLog = log;
                            }
//                            if (lastLog != null) {
//                                lastLog.swipe(Direction.UP, 1.0f, 2);
//                            } else {
//                                break;
//                            }
                            //}
                            ipIsSaved = true;
                        } else {
                            break;
                        }
                    } else {//mission collect or no mission
                        if(!isCollect) {
                            while (!mDevice.hasObject(By.res(PACKAGE, "app_hardware")) &&
                                    mDevice.hasObject(By.res(PACKAGE, "ct_login_name"))) {
                                sleep(100);
                            }
                            try {
                                mDevice.wait(Until.findObject(By.res(PACKAGE, "app_hardware")), 1000);
                                click("app_hardware");
                            } catch (NullPointerException e) {
                                Log.w("MyTag", "NPE");
                                break;
                            }
                            mDevice.wait(Until.findObject(By.res(PACKAGE, "rth_btn")), 10000);
                            List<UiObject2> miners = mDevice.findObjects(By.res(PACKAGE, "rth_btn"));
                            int i1 = miners.size();
                            if ((nextClient.getAction() != null) && (nextClient.getAction().equals(Client.COLLECT))) {//mission COLLECT
                                int hackCoinsBefore = getMyMoney();
                                for (int i2 = 0; i2 < i1; i2++) {
                                    miners.get(i2).click();
                                    waitAndClick("whois_done");
                                    waitObj("rth_btn");
                                    int diff = getMyMoney() - hackCoinsBefore;
                                    if (diff > 0) {
                                        nextClient.setAction(Client.NOTHING);
                                    }
                                    miners = mDevice.findObjects(By.res(PACKAGE, "rth_btn"));
                                    rests = getRestSeconds();
                                    if (rests < 7) {
                                        break;
                                    }
                                }
                            } else {//no mission
                                for (int i2 = 0; i2 < i1; i2++) {
                                    miners.get(i2).click();
                                    waitAndClick("whois_done");
                                    waitObj("rth_btn");
                                    miners = mDevice.findObjects(By.res(PACKAGE, "rth_btn"));
                                    rests = getRestSeconds();
                                    if (rests < 7) {
                                        break;
                                    }
                                }
                            }
                            isCollect = true;
                            rests = getRestSeconds();
                        } else if (!logIsDeleted) {
                            waitAndClick("app_log");//Лог
                            mDevice.wait(Until.findObject(By.res(PACKAGE, "log_item_ip")), 1000);//ip в логах
                            List<UiObject2> logs = mDevice.findObjects(By.res(PACKAGE, "log_item_ip"));//ip в логах
                            int countLogs = logs.size();
                            for (int i1 = 0; i1 < countLogs; i1++) {
                                UiObject2 log = logs.get(i1);
                                if (log.getText().equals("53.95.120.218")) {
                                    log.getParent().findObject(By.res(PACKAGE, "log_item_del")).click(); //удалить лог
                                    i1--;
                                    sleep(1000);
                                }
                                waitObj("btn_disconnect");//кнопка отключения не видна изза всплывающего окна
                                logs = mDevice.findObjects(By.res(PACKAGE, "log_item_ip"));//ip в логах
                                countLogs = logs.size();
                                rests = getRestSeconds();
                                if (rests < 7) {
                                    break;
                                }
                            }
                            logIsDeleted = true;
                            rests = getRestSeconds();
                        } else if (!ipIsSaved){
                            //for (int i2 = 0; i2 < 1; i2++) {
                            List<UiObject2> logs = mDevice.findObjects(By.res(PACKAGE, "log_item_ip"));//ip в логах
//                            UiObject2 lastLog = null;
                            for (UiObject2 log : logs) {
                                saveIP(log.getText());
                                //lastLog = log;
                            }
//                            if (lastLog != null) {
//                                lastLog.swipe(Direction.UP, 1.0f, 2);
//                            } else {
//                                break;
//                            }
                            //}
                            ipIsSaved = true;
                        } else {
                            break;
                        }
                    }
                }
                waitAndClick("btn_disconnect");//отключение
                String s2 = waitAndGetText("whois");
                while (!s2.endsWith("!")) {
                    sleep(200);
                    s2 = waitAndGetText("whois");
                }
                waitObj("whois_done");
                sleep(1000);
                waitAndClick("whois_done");//закрыть всплывающее окно
                waitAndClick("bounce_delete");//закрыть ip слева
                sleep(500);
                int x = 1000;
                int y = 60;
                mDevice.swipe(x, y, x, y, 80);
                sleep(500);
            } else {
                waitAndClick("whois_done");
            }
        }
        //недостижимый код
    }

    private int getRestSeconds() {
        String strRest = waitAndGetText("ct_trace");
        if (strRest.contains(":") || strRest.contains(" не ")) {
            return 60;
        } else {
            return Integer.parseInt(strRest);
        }

    }

    private boolean clientIsValid(Client client, String string) {
        Pattern p3 = Pattern.compile("Репутация:\\s.+\\n\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*");
        Matcher m3 = p3.matcher(string);
        m3.find();
        if (string.contains("Invalid IP")) {
            return false;
        }
        int rep = Integer.parseInt(string.substring(m3.start() + 11, m3.end() - 12));

        if ((rep < myRep*MIN_REP || rep > myRep*MAX_REP) && (client.getAction() == null || client.getAction().equals(""))) {//not valid
            client.setRep(rep);

            Pattern patternOwner = Pattern.compile("Target\\sname:.+'s\\sGateway");
            Matcher matcherOwner = patternOwner.matcher(string);
            matcherOwner.find();
            client.setOwner(string.substring(matcherOwner.start() + 13, matcherOwner.end() - 10));

            Pattern patternLevel = Pattern.compile("Risk:\\sLevel\\s.+\\s\\|\\s");
            Matcher matcherLevel = patternLevel.matcher(string);
            matcherLevel.find();
            client.setLevel(Integer.parseInt(string.substring(matcherLevel.start() + 12, matcherLevel.end() - 3)));

            client.setLastCrack(new Date());

            Pattern patternGuild = Pattern.compile("Гильдия:\\s.+\\nРепутация");
            Matcher matcherGuild = patternGuild.matcher(string);
            if (matcherGuild.find()) {
                client.setGuild(string.substring(matcherGuild.start() + 9, matcherGuild.end() - 10));
            }
            if (!clients.contains(client)) {
                clients.offer(client);
            }
            return false;
        } else {//valid
            client.setRep(rep);

            Pattern patternOwner = Pattern.compile("Target\\sname:.+'s\\sGateway");
            Matcher matcherOwner = patternOwner.matcher(string);
            matcherOwner.find();
            client.setOwner(string.substring(matcherOwner.start() + 13, matcherOwner.end() - 10));

            Pattern patternLevel = Pattern.compile("Risk:\\sLevel\\s.+\\s\\|\\s");
            Matcher matcherLevel = patternLevel.matcher(string);
            matcherLevel.find();
            client.setLevel(Integer.parseInt(string.substring(matcherLevel.start() + 12, matcherLevel.end() - 3)));

            client.setLastCrack(new Date());

            Pattern patternGuild = Pattern.compile("Гильдия:\\s.+\\nРепутация");
            Matcher matcherGuild = patternGuild.matcher(string);
            if (matcherGuild.find()) {
                client.setGuild(string.substring(matcherGuild.start() + 9, matcherGuild.end() - 10));
            }

            if (!clients.contains(client)) {
                clients.offer(client);
            }

            writeClientsToDisc();
            return true;
        }
    }

    private Client getNextClient() {
        Client nextClient = clients.poll();
        Date lastCrack = nextClient.getLastCrack();
        int countTries = 0;
        if (IS_LAUNCHED) {
            collectMissions();
            writeClientsToDisc();
            writeMissionsToDisc();
            IS_LAUNCHED = false;
        }

        while (lastCrack != null) {
            Log.w("MyTag", "NextClient try: " + countTries);
            if (countTries > clients.size()) {
                collectMissions();
                writeClientsToDisc();
                writeMissionsToDisc();
                countTries = 0;
            }
            long afterCrack = new Date().getTime() - lastCrack.getTime();
            int rep = nextClient.getRep();
            if (afterCrack > (1000*60*60 + 20000)) {//если был взломан более часа назад
                if (nextClient.getAction() != null && !nextClient.getAction().equals("")) {
                    return nextClient;
                }
                if ((rep > myRep*MIN_REP && rep < myRep*MAX_REP) || rep == 0) {
                    return nextClient;
                }
            }
            clients.offer(nextClient);
            sleep(100);
            nextClient = clients.poll();
            countTries++;
            lastCrack = nextClient.getLastCrack();
        }
        return nextClient;
    }

   private void collectMissions() {
       waitAndClick("img_missions");//задания
       waitObj("row");
       sleep(1000);

       int restsMissions = collectStartedMissions();

       waitAndClick("btn_mission_public");//кнопка поиск заданий
       waitObj("lv_p_mission");
       mDevice.findObject(By.res(PACKAGE, "lv_p_mission")).scroll(Direction.UP, 0.5f, 700);//список заданий поднимаем в начало

       for (int i = 0; i < 2; i++) {
           waitObj("mission_i__title");
           int countMissions = mDevice.findObject(By.res(PACKAGE, "lv_p_mission")).getChildCount();
           for (int i1 = 0; i1 < countMissions; i1++) {
               if (restsMissions == 0) {
                   break;
               }
               waitObj("mission_i__title");
               UiObject2 title = mDevice.findObjects(By.res(PACKAGE, "mission_i__title")).get(i1);
               String missionTitle = title.getText();
               Log.w("MyTag", "mission in process" + missionTitle);
               if (missionTitle.startsWith("Collect from") || missionTitle.startsWith("Delete logs")) {
                   Date date = missions.get(missionTitle);
                   if (date != null) {//миссия уже выполнялась
                       if ((new Date().getTime() - date.getTime()) > 1000*60*60*4) {//прошло 4 часа
                           if (startMission(title, missionTitle)) {
                               restsMissions--;
                           }
                       }
                   } else {//миссия не выполнялась
                       if (startMission(title, missionTitle)) {
                           restsMissions--;
                       }
                   }
               } else if (missionTitle.startsWith("Do you have IP")) {
                   Log.w("MyTag", "doyouhave TODO");
               } else {//unknown mission
                   Log.w("MyTag", "unknown mission: " + missionTitle);
               }
           }
           if (restsMissions == 0) {
               break;
           } else {
               waitObj("lv_p_mission");
               mDevice.findObject(By.res(PACKAGE, "lv_p_mission")).scroll(Direction.DOWN, 0.7f, 600);//список заданий поднимаем в конец
           }
       }
       collectStartedMissions();
       waitAndClick("img_connection");
   }

    /** собирает инфу о взятых заданиях */
    private int collectStartedMissions() {
        waitAndClick("btn_mission_my");//кнопка Активные задания
        waitObj("mission_i__title");
        int countMyMissions = mDevice.findObject(By.res(PACKAGE, "lv_mission")).getChildCount();
        for (int i = 0; i < countMyMissions; i++) {
            waitObj("mission_i__title");
            UiObject2 title = mDevice.findObjects(By.res(PACKAGE, "mission_i__title")).get(i);
            String missionTitle = title.getText();
            if (!missions.containsKey(missionTitle)) {
                if (missionTitle.startsWith("Collect from") || missionTitle.startsWith("Delete logs")) {
                    missions.put(missionTitle, new Date());
                    title.click();
                    waitObj("mission_det_description");//обработка всплывающего окна
                    String ip = waitAndGetText("mission_det_target");
                    Client client = new Client();
                    client.setIp(ip);
                    String action = "";
                    if (missionTitle.startsWith("Collect from")) {
                        action = Client.COLLECT;
                    } else if (missionTitle.startsWith("Delete logs")) {
                        action = Client.DELETE;
                    }
                    if (!clients.contains(client)) {
                        client.setAction(action);
                        clients.offer(client);
                    } else {
                        for (Client c : clients) {
                            if (c.getIp().equals(ip)) {
                                c.setAction(action);
                            }
                        }
                    }
                    waitAndClick("btn_done");
                } else if (missionTitle.startsWith("Do you have IP")) {
                    missions.put(missionTitle, new Date());
                } else {//unknown mission
                    missions.put(missionTitle, new Date());
                    Log.w("MyTag", "unknown mission: " + missionTitle);
                }
            }
        }
        return 10 - countMyMissions;
    }

   /** обработка окна задания в поиске заданий */
   private boolean startMission(UiObject2 title, String missionTitle) {
       title.click();
       waitObj("mission_det_description");//обработка всплывающего окна
       //mDevice.swipe(1000, 800, 1000, 600, 20);
       waitObj("mission_det_target_rep");
       String strRep = mDevice.findObject(By.res(PACKAGE, "mission_det_target_rep")).getText();
       strRep = strRep.substring(0, strRep.indexOf(' '));
       int rep = Integer.parseInt(strRep);
       if (rep > myRep*MAX_REP){//not valid
           click("btn_done");
           return false;
       } else {//valid
           click("btn_start");
           missions.put(missionTitle, new Date());
           while (!mDevice.hasObject(By.res(PACKAGE, "btn_mission_public")) && !mDevice.hasObject(By.res("android", "button1"))) {
               sleep(800);
           }
           if (mDevice.hasObject(By.res(PACKAGE, "btn_mission_public"))) {
               click("btn_mission_public");
               return true;
           } else if (mDevice.hasObject(By.res("android", "button1"))){
               if (mDevice.hasObject(By.res(PACKAGE, "app_input_prompt"))) {//капча
                   Log.w("MyTag" , "captcha");
                   String s = waitAndGetText("app_input_prompt");
                   Pattern p = Pattern.compile("!:\\s[0-9]{3}");
                   Matcher m = p.matcher(s);
                   m.find();
                   mDevice.findObject(By.res(PACKAGE, "app_input_data")).setText(s.substring(m.start() + 3, m.end()));//ввод капчи
                   //возможен баг
               } else {
                   Log.w("MyTag", "4 hour expected");

               }
               mDevice.findObject(By.res("android", "button1")).click();
               return false;
           }
           return false;
       }
   }



    private int getMyMoney() {
        String hc = mDevice.findObject(By.res(PACKAGE, "stat_cash")).getText();
        hc = hc.replaceAll("\\s", "").replaceAll("HC", "");
        return Integer.parseInt(hc);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
        }
    }

    private void initSavedIP() {
        File sdcard = Environment.getExternalStorageDirectory();
        File file = new File(sdcard, PATH_SAVED_CLIENTS);
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                String[] arr = line.split("\\sзх\\s");
                String ip = "";
                int rep = 0;
                String owner = "";
                int level = 0;
                Date lastCrack = null;
                String guild = "";
                String action = "";
                for (int i = 0; i < arr.length; i++) {
                    switch (i) {
                        case 0: {
                                    ip = arr[0];
                                    break;
                                }
                        case 1: {
                                    if (!arr[1].equals("")){
                                        rep = Integer.parseInt(arr[1]);
                                    }
                                    break;
                                }
                        case 2: {
                                    owner = arr[2];
                                    break;
                                }
                        case 3: {
                                    if (!arr[3].equals("")) {
                                        level = Integer.parseInt(arr[3]);
                                    }
                                    break;
                                }
                        case 4: {
                                    if (!arr[4].equals("")) {
                                        lastCrack = dateFormat.parse(arr[4]);
                                    }
                                    break;
                                }
                        case 5: {
                                    guild = arr[5];
                                    break;
                                }
                        case 6: {
                                    action = arr[6];
                                    //action ="";
                                    break;
                                }

                    }
                }
                Client client = new Client(ip, rep, owner, level, lastCrack, guild, action);
                if (!clients.contains(client)) {
                    clients.offer(client);
                }
            }
            br.close();
        }
        catch (IOException e) {
            try {
                file.createNewFile();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private void initSavedMissions() {
        File sdcard = Environment.getExternalStorageDirectory();
        File file = new File(sdcard, PATH_SAVED_MISSIONS);
        long now = new Date().getTime();
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                String[] arr = line.split("\\sзх\\s");
                String mission = "";
                Date lastGet = null;
                for (int i = 0; i < arr.length; i++) {
                    switch (i) {
                        case 0: {
                            mission = arr[0];
                            break;
                        }
                        case 1: {
                            if (!arr[1].equals("")) {
                                lastGet = dateFormat.parse(arr[1]);
                            }
                            break;
                        }
                    }
                }
                if ((lastGet != null) && (now - lastGet.getTime()) < 1000*60*60*4) {
                    missions.put(mission, lastGet);
                }
            }
            br.close();
        }
        catch (IOException e) {
            try {
                file.createNewFile();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private void saveIP(String ip) {
        Client c = new Client();
        c.setIp(ip);
        if (!ip.equals("Bounced IP") && !clients.contains(c) && !ip.equals(MY_IP)) {
            if (ip.endsWith(" [Уже взломан]")) {
                ip = ip.substring(0, ip.length() - 14);
                c.setIp(ip);
                c.setLastCrack(new Date());
            }
            clients.offer(c);
        }
    }

    private void writeClientsToDisc() {
        try {
            File sdcard = Environment.getExternalStorageDirectory();
            File file = new File(sdcard, PATH_SAVED_CLIENTS);
            PrintWriter writer = new PrintWriter(file);
            writer.print("");
            writer.close();
            BufferedWriter bw = new BufferedWriter(new FileWriter(file, true));
            for (Client client : clients){
                String rep = "";
                if (client.getRep() != 0) {
                    rep = String.valueOf(client.getRep());
                }
                String owner = "";
                if (client.getOwner() != null) {
                    owner = client.getOwner();
                }
                String level = "";
                if (client.getLevel() != 0) {
                    level = String.valueOf(client.getLevel());
                }
                String lastCrack = "";
                if (client.getLastCrack() != null) {
                    lastCrack = dateFormat.format(client.getLastCrack());
                }
                String guild = "";
                if (client.getGuild() != null) {
                    guild = client.getGuild();
                }
                String action = "";
                if (client.getAction() != null) {
                    action = client.getAction();
                }
                bw.write(client.getIp() + " зх " + rep + " зх " + owner + " зх " + level + " зх " +
                        lastCrack + " зх " + guild + " зх " + action + " зх " + "\n");
            }
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeMissionsToDisc() {
        try {
            File sdcard = Environment.getExternalStorageDirectory();
            File file = new File(sdcard, PATH_SAVED_MISSIONS);
            PrintWriter writer = new PrintWriter(file);
            writer.print("");
            writer.close();
            BufferedWriter bw = new BufferedWriter(new FileWriter(file, true));
            long now = new Date().getTime();
            for (Map.Entry<String, Date> entry: missions.entrySet()) {
                if ((now - entry.getValue().getTime()) < 1000*60*60*4) {
                    bw.write(entry.getKey() + " зх " + dateFormat.format(entry.getValue()) + " зх " + "\n");
                }
            }
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean waitAndIsExist(String resIdWait, String resIdIsExist) {
        waitObj(resIdWait);
        return mDevice.hasObject(By.res(PACKAGE, resIdIsExist));
    }

    private String waitAndGetText(String resourceId) {
        return mDevice
                .wait(Until.findObject(By.res(PACKAGE, resourceId)), 30000)
                .getText();
    }

    private void click(String resourceId) {
        mDevice.findObject(By.res(PACKAGE, resourceId)).click();
    }

    private void waitObj(String resourceId){
        mDevice.wait(Until.findObject(By.res(PACKAGE, resourceId)), 30000);
    }

    private void waitAndClick(String resourceId) {
        mDevice.wait(Until.findObject(By.res(PACKAGE, resourceId)), 60000);
        mDevice.findObject(By.res(PACKAGE, resourceId)).click();
    }

    private class Client {
        public static final String NOTHING = "";
        public static final String COLLECT = "c";
        public static final String DELETE = "d";
        public static final String DO_YOU_HAVE = "i";

        private String ip;
        private int rep;
        private String owner;
        private int level;
        private Date lastCrack;
        private String guild;
        private String action;

        /**
         * action:
         * i - do you know ip
         * d - delete log from
         * c - collect from
         * empty - default action
         */

        //Constructors
        public Client() {
        }
        public Client(String ip, int rep, String owner, int level, Date lastCrack, String guild, String action) {
            this.ip = ip;
            this.rep = rep;
            this.owner = owner;
            this.level = level;
            this.lastCrack = lastCrack;
            this.guild = guild;
            this.action = action;
        }

        //Getters and Setters
        public String getIp() {
            return ip;
        }
        public void setIp(String ip) {
            this.ip = ip;
        }
        public int getRep() {
            return rep;
        }
        public void setRep(int rep) {
            this.rep = rep;
        }
        public String getOwner() {
            return owner;
        }
        public void setOwner(String owner) {
            this.owner = owner;
        }
        public int getLevel() {
            return level;
        }
        public void setLevel(int level) {
            this.level = level;
        }
        public Date getLastCrack() {
            return lastCrack;
        }
        public void setLastCrack(Date lastCrack) {
            this.lastCrack = lastCrack;
        }
        public String getGuild() {
            return guild;
        }
        public void setGuild(String guild) {
            this.guild = guild;
        }
        public String getAction() {
            return action;
        }
        public void setAction(String action) {
            this.action = action;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Client client = (Client) o;

            return ip.equals(client.ip);

        }

        @Override
        public int hashCode() {
            return ip.hashCode();
        }

        @Override
        public String toString() {
            return ip + " зх " +
                    rep + " зх " +
                    owner + " зх " +
                    level + " зх " +
                    lastCrack + " зх " +
                    guild + " зх " +
                    action;
        }
    }
}