import com.sun.mail.util.MailSSLSocketFactory;
import org.apache.commons.io.FileUtils;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

public class Main {
    public static final String INFO_PATH_WIN = "D://data.txt";      //存放学生信息的文件路径 Windows
    public static final String INFO_PATH_LINUX = "/root/info/data.txt";      //存放学生信息的文件路径 Linux
    public static final String INFO_PATH = INFO_PATH_LINUX;      //存放学生信息的文件路径
    public static final int DAYS_NOTICE = 2;    //当作业剩余时间小于DAYS_NOTICE时，发送提醒邮件。

    public static void main(String[] args) throws GeneralSecurityException {
        System.out.println("---"+LocalDateTime.now()+"---");
        Map<String, String[]> info = readInfo();
        for (String[] value : info.values()) {
            System.out.println("---"+value[0]+"---");
            work(value[1], value[2], value[3]);
        }


    }

    public static void work(String account, String psw, String mail) throws GeneralSecurityException {
        WorkQuery workQuery = new WorkQuery(account, psw);
        workQuery.login();
        String reminder = workQuery.getReminder();
        List<String> lessonUrls = workQuery.getLessonUrls(reminder);
        List<String> homeworkUrls;
        List<String> outs = new ArrayList<>();      //需要提醒的作业的输出
        List<String> outAll = new ArrayList<>();    //全部作业的输出
        int flag = Integer.MAX_VALUE;   //用来判断作业截止的紧急情况，0表示有作业今天截止，1表示有作业明天截止。
        for (String lessonUrl : lessonUrls) {
            String lessonPage = workQuery.lessonPage(lessonUrl);
            homeworkUrls = workQuery.getHomeworkUrls(lessonPage);
            String teacherName = workQuery.getTeacherName(lessonPage);
            for (String homeworkUrl : homeworkUrls) {
                String homeworkPage = workQuery.homeworkPage(homeworkUrl);
                String homeworkName = workQuery.getHomeworkName(homeworkPage);
                String deadLine = workQuery.getDeadLine(homeworkPage);
                if (needNotice(deadLine)) {
                    outs.add(teacherName + "发布的作业:" + homeworkName + "，截止日期为：" + deadLine +
                            " ，还剩余" + getRemaingTime(deadLine)[0] + "天" + getRemaingTime(deadLine)[1] + "小时。<br>");
                    if (getRemaingTime(deadLine)[0] < flag) {
                        flag = Math.toIntExact(getRemaingTime(deadLine)[0]);
                    }
                }
                if (getRemaingTime(deadLine)[1] > 0) {
                    outAll.add(teacherName + "发布的作业:" + homeworkName + "，截止日期为：" + deadLine +
                            " ，还剩余" + getRemaingTime(deadLine)[0] + "天" + getRemaingTime(deadLine)[1] + "小时。<br>");
                }
            }
        }

        System.out.println("---即将截止的作业---");
        StringBuilder result = new StringBuilder();
        for (String out : outs) {
            result.append(out);
        }
        System.out.println(result);
        System.out.println("---全部作业---");
        for (String all : outAll) {
            System.out.println(all);
        }
        if (flag == 0) {
            sendMail(mail, "您有作业今天截止！", result.toString());
        } else if (flag != Integer.MAX_VALUE) {
            sendMail(mail, "您有作业" + flag + "天后截止！", result.toString());
        }
        workQuery.close();
    }

    /**
     * 读取学生的信息
     */
    public static Map<String, String[]> readInfo() {
        Map<String, String[]> infos = new LinkedHashMap<>();
        File file = new File(INFO_PATH);
        List<String> lines = null;
        try {
            lines = FileUtils.readLines(file, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
        assert lines != null;
        for (String line : lines) {
            String[] data = line.split(" ");
            infos.put(data[0], data);
        }
        return infos;
    }

    /**
     * 判断是否需要提醒
     *
     * @param deadLine 作业截止日期
     */
    public static boolean needNotice(String deadLine) {
        Long[] remaingTime = getRemaingTime(deadLine);
        //表示还有一天多的时间去完成作业，提前通知一下。
        return remaingTime[0] >= 0 && remaingTime[0] < DAYS_NOTICE && remaingTime[1] > 0;
    }

    /**
     * 计算距离作业截止还有多久的时间
     *
     * @param deadLine 截止日期
     */
    public static Long[] getRemaingTime(String deadLine) {
        String[] split = deadLine.split("[年月日]");
        LocalDateTime end = LocalDateTime.of(Integer.parseInt(split[0]), Integer.parseInt(split[1]), Integer.parseInt(split[2]), 23, 59, 59);
        LocalDateTime now = LocalDateTime.now();
        Duration duration = Duration.between(now, end);
        long days = duration.toDays();
        duration = Duration.between(now.plusDays(days), end);
        long hours = duration.toHours();
        Long[] remaingTime = new Long[2];
        remaingTime[0] = days;
        remaingTime[1] = hours;
        return remaingTime;
    }

    public static void sendMail(String to, String title, String content) throws GeneralSecurityException {
        //设置发送邮件的主机  smtp.qq.com
        String host = "smtp.qq.com";
        //1.创建连接对象，连接到邮箱服务器
        Properties props = System.getProperties();
        //Properties 用来设置服务器地址，主机名 。。 可以省略
        //设置邮件服务器
        props.setProperty("mail.smtp.host", host);
        props.put("mail.smtp.auth", "true");
        //SSL加密
        MailSSLSocketFactory sf = new MailSSLSocketFactory();
        sf.setTrustAllHosts(true);
        props.put("mail.smtp.ssl.enable", "true");
        props.put("mail.smtp.ssl.socketFactory", sf);
        //props：用来设置服务器地址，主机名；Authenticator：认证信息
        Session session = Session.getDefaultInstance(props, new Authenticator() {
            @Override
            //通过密码认证信息
            protected PasswordAuthentication getPasswordAuthentication() {
                //new PasswordAuthentication(用户名, password);
                //这个用户名密码就可以登录到邮箱服务器了,用它给别人发送邮件
                return new PasswordAuthentication("haoyun.zheng@foxmail.com", "oeldhqpxsunmbcjd");
            }
        });
        try {
            Message message = new MimeMessage(session);
            //2.1设置发件人：
            message.setFrom(new InternetAddress("haoyun.zheng@foxmail.com"));
            //2.2设置收件人 这个TO就是收件人
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
            //2.3邮件的主题
            message.setSubject(title);
            //2.4设置邮件的正文 第一个参数是邮件的正文内容 第二个参数是：是文本还是html的连接
            message.setContent(content, "text/html;charset=UTF-8");
            //3.发送一封激活邮件
            Transport.send(message);

        } catch (MessagingException mex) {
            mex.printStackTrace();
        }

    }
}
