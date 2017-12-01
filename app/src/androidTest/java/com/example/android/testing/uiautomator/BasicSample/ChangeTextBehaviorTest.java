package com.example.android.testing.uiautomator.BasicSample;

import org.junit.Before;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Basic sample for unbundled UiAutomator.
 */
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 18)
public class ChangeTextBehaviorTest {

    private static final String PACKAGE = "net.okitoo.hackers";
    private static final int LAUNCH_TIMEOUT = 5000;
    private static final String PATH_SAVED_CLIENTS = "Download/MyBot/SavedClients.txt";
    private static final float MAX_REP = 1.6f;
    private static final float MIN_REP = 0.76f;

    private UiDevice mDevice;
    private Queue<Client> clients = new LinkedList<>();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
    private int myRep = 0;

    @Before
    public void startMainActivityFromHomeScreen() {
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        mDevice.pressHome();

        final String launcherPackage = getLauncherPackageName();
        assertThat(launcherPackage, notNullValue());
        mDevice.wait(Until.hasObject(By.pkg(launcherPackage).depth(0)), LAUNCH_TIMEOUT);

        // Launch the blueprint app
        Context context = InstrumentationRegistry.getContext();
        final Intent intent = context.getPackageManager()
                .getLaunchIntentForPackage(PACKAGE);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);    // Clear out any previous instances
        context.startActivity(intent);

        // Wait for the app to appear
        mDevice.wait(Until.hasObject(By.pkg(PACKAGE).depth(0)), LAUNCH_TIMEOUT);
    }

    @Test
    public void runBot() throws UiObjectNotFoundException {
//        waitAndClick("img_hardware");
//        mDevice.wait(Until.findObject(By.res(PACKAGE, "lv_hardware")), 30000);
//        UiScrollable hardList = new UiScrollable(
//                new UiSelector()
//                        .resourceId("net.okitoo.hackers:id/lv_hardware")
//                        .scrollable(true)
//        );
//        hardList.setAsVerticalList();
//        hardList.scrollToEnd(3);
//        waitAndClick("btn_func");                           //collect
        initSavedIP();
        mDevice.wait(Until.findObject(By.res(PACKAGE, "whois_done")), 120000);
        mDevice.findObject(By.res(PACKAGE, "whois_done")).click();
        //waitAndClick("whois_done");
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
            int x = 1700;
            int y = 200;
            mDevice.swipe(x, y, x, y, 80);
            sleep(2000);
        }
        writeToDisc();

        //while true
        for (int i = 0; i < 1000 ; i++) {
            String nextIP = getNextIP();
            waitObj("connection_new_target");
            mDevice.findObject(By.res(PACKAGE, "connection_new_target")).setText("");
            mDevice.findObject(By.res(PACKAGE, "connection_new_target")).setText(nextIP);
            click("connections_add");
            sleep(1000);//try to delete


            while (!mDevice.hasObject(By.res("android", "button1")) && !mDevice.hasObject(By.res(PACKAGE, "whois"))) {
                sleep(500);
            }
            if (mDevice.hasObject(By.res("android", "button1"))) {
                if(mDevice.hasObject(By.res(PACKAGE, "app_input_prompt"))) {//капча
                    Log.w("MyTag" , "app_input finds");
                    String s = waitAndGetText("app_input_prompt");
                    Pattern p = Pattern.compile("!:\\s[0-9]{3}");
                    Matcher m = p.matcher(s);
                    m.find();
                    mDevice.findObject(By.res(PACKAGE, "app_input_data")).setText(s.substring(m.start() + 3, m.end()));//ввод капчи
                    mDevice.findObject(By.res("android", "button1")).click();
                    waitAndClick("whois_done");
                    continue;
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
            if (clientIsValid(nextIP, s)) {
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
                        int x = 1700;
                        int y = 200;
                        mDevice.swipe(x, y, x, y, 80);//закрыть ip в списке справа
                        sleep(500);
                        continue;
                    }
                }
                waitObj("software_i_icon");
                sleep(200);
                mDevice.findObjects(By.res(PACKAGE, "software_i_icon")).get(1).click();//взломать 2
                sleep(1000);
                boolean isCollect = false;
                boolean logIsDeleted = false;
                boolean ipIsSaved = false;
                int rests = Integer.parseInt(waitAndGetText("ct_trace"));
                while(rests > 8) {//отслеживание
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
                       // waitAndClick("app_hardware");//оборудование !!!!ошибка: не возникает изображеине с 3мя кнопками
                        List<UiObject2> miners = mDevice.findObjects(By.res(PACKAGE, "rth_btn"));
                        int i1 = miners.size();
                        for (int i2 = 0; i2 < i1; i2++) {
                            miners.get(0).click();
                            //sleep(1800);
                            //mDevice.wait(Until.findObject(By.res(PACKAGE, "whois_done")), 5000);//закрыть вспывающее окно
                            //sleep(600);
                            //click("whois_done");

                            waitAndClick("whois_done");

                            waitObj("rth_btn");
                            miners = mDevice.findObjects(By.res(PACKAGE, "rth_btn"));
                            rests = Integer.parseInt(waitAndGetText("ct_trace"));
                            if (rests < 7) {
                                break;
                            }
                        }
                        isCollect = true;
                        rests = Integer.parseInt(waitAndGetText("ct_trace"));
                    } else if (!logIsDeleted) {
                        waitAndClick("app_log");//Лог
                        mDevice.wait(Until.findObject(By.res(PACKAGE, "log_item_ip")), 1000);//ip в логах
                        List<UiObject2> logs = mDevice.findObjects(By.res(PACKAGE, "log_item_ip"));//ip в логах
                        int countLogs = logs.size();
                        for (int i1 = 0; i1 < countLogs; i1++) {
                            UiObject2 log = logs.get(i1);
                            if (log.getText().equals("53.95.120.218")) {
                                log.getParent().findObject(By.res(PACKAGE, "log_item_del")).click(); //удалить лог
                                sleep(1000);
                            }
                            waitObj("btn_disconnect");//кнопка отключения не видна изза всплывающего окна
                            logs = mDevice.findObjects(By.res(PACKAGE, "log_item_ip"));//ip в логах
                            countLogs = logs.size();
                            rests = Integer.parseInt(waitAndGetText("ct_trace"));
                            if (rests < 7) {
                                break;
                            }
                        }
                        logIsDeleted = true;
                        rests = Integer.parseInt(waitAndGetText("ct_trace"));
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
                int x = 1700;
                int y = 200;
                mDevice.swipe(x, y, x, y, 80);//400
                sleep(500);
            } else {
                waitAndClick("whois_done");
            }
        }
        writeToDisc();



//        try {
//            File sdcard = Environment.getExternalStorageDirectory();
//            File file = new File(sdcard, "Download/MyBot/Test.txt");
//            file.createNewFile();
//            PrintWriter writer = new PrintWriter(file);
//            writer.print("");
//            writer.close();
//            BufferedWriter bw = new BufferedWriter(new FileWriter(file, true));
//            bw.write(s);
//            bw.close();
//        } catch (IOException e) {
//        }


        //сейчас в дисп подключ

        assertThat(mDevice, notNullValue());
    }

//    private int parceRest(String s) {
//
//    }

    private boolean clientIsValid(String ip, String string) {
        Pattern p3 = Pattern.compile("Репутация:\\s.+\\n\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*");
        Matcher m3 = p3.matcher(string);
        m3.find();
        if (string.contains("Invalid IP")) {
            return false;
        }
        int rep = Integer.parseInt(string.substring(m3.start() + 11, m3.end() - 12));

        if (rep < myRep*MIN_REP || rep > myRep*MAX_REP) {
//            Client client = new Client();
//            client.setIp(ip);
//            client.setRep(rep);
//            Pattern p = Pattern.compile("Target\\sname:.+'s\\sGateway");
//            Matcher m = p.matcher(string);
//            m.find();
//            client.setOwner(string.substring(m.start() + 13, m.end() - 10));
//            Pattern p2 = Pattern.compile("Risk:\\sLevel\\s.+\\s\\|\\s");
//            Matcher m2 = p2.matcher(string);
//            m2.find();
//            client.setLevel(Integer.parseInt(string.substring(m2.start() + 12, m2.end() - 3)));
//            client.setLastCrack(new Date());
//            if (!clients.contains(client)) {
//                clients.offer(client);
//            }
            //not valid
            return false;
        } else {
            //valid
            Client client = new Client();
            client.setIp(ip);
            client.setRep(rep);
            Pattern p = Pattern.compile("Target\\sname:.+'s\\sGateway");
            Matcher m = p.matcher(string);
            m.find();
            client.setOwner(string.substring(m.start() + 13, m.end() - 10));
            Pattern p2 = Pattern.compile("Risk:\\sLevel\\s.+\\s\\|\\s");
            Matcher m2 = p2.matcher(string);
            m2.find();
            client.setLevel(Integer.parseInt(string.substring(m2.start() + 12, m2.end() - 3)));
            client.setLastCrack(new Date());
            if (!clients.contains(client)) {
                clients.offer(client);
            }
            writeToDisc();
            return true;
        }
    }

    private String getNextIP() {
        Client nextClient = clients.poll();
        Date lastCrack = nextClient.getLastCrack();
        while (lastCrack != null) {
            long afterCrack = new Date().getTime() - lastCrack.getTime();
            if (afterCrack > (1000*60*60 + 20000)) {
                return nextClient.getIp();
            } else {
                clients.offer(nextClient);
                sleep(1000);
                nextClient = clients.poll();
                lastCrack = nextClient.getLastCrack();
            }
        }
        return nextClient.getIp();
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
                if (arr.length == 1) {
                    Client c = new Client();
                    c.setIp(arr[0]);
                    if (!clients.contains(c)) {
                        clients.offer(c);
                    }
                } else if (arr.length == 5) {
                    int rep = 0;
                    if (!arr[1].equals("")){
                        rep = Integer.parseInt(arr[1]);
                    }
                    int level = 0;
                    if (!arr[3].equals("")) {
                        level = Integer.parseInt(arr[3]);
                    }
                    Date date = null;
                    if (!arr[4].equals("")) {
                        date = dateFormat.parse(arr[4]);
                    }
                    Client c = new Client(arr[0], rep, arr[2], level, date);
                    if (!clients.contains(c)) {
                        clients.offer(c);
                    }
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
        if (!ip.equals("Bounced IP") && !clients.contains(c) && !ip.equals("53.95.120.218")) {
            if (ip.endsWith(" [Уже взломан]")) {
                ip = ip.substring(0, ip.length() - 14);
                c.setIp(ip);
                c.setLastCrack(new Date());
            }
            clients.offer(c);
        }
    }

    private void writeToDisc() {
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
                bw.write(client.getIp() + " зх " + rep + " зх " + owner + " зх " + level + " зх " + lastCrack + "\n");
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
        private String ip;
        private int rep;
        private String owner;
        private int level;
        private Date lastCrack;
        private String guild;

        //Constructors
        public Client() {
        }
        public Client(String ip, int rep, String owner, int level, Date lastCrack) {
            this.ip = ip;
            this.rep = rep;
            this.owner = owner;
            this.level = level;
            this.lastCrack = lastCrack;
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
            return ip +" зх " +
                    rep +" зх " +
                    owner +" зх " +
                    level +" зх " +
                    lastCrack;
        }
    }

    private String getLauncherPackageName() {
        final Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);

        PackageManager pm = InstrumentationRegistry.getContext().getPackageManager();
        ResolveInfo resolveInfo = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return resolveInfo.activityInfo.packageName;
    }
}
