import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WorkQuery {

    private String userAccount = "201750080616";
    private String userPassword = "164278";
    private CloseableHttpClient httpClient;
    private CloseableHttpResponse response;
    private HttpGet httpGet;

    public WorkQuery() {
    }

    public WorkQuery(String userAccount, String userPassword) {
        this.userAccount = userAccount;
        this.userPassword = userPassword;
    }

    public String getLoginToken() {
        try {
            httpClient = HttpClients.createDefault();
            httpGet = new HttpGet("http://pt.csust.edu.cn/meol/homepage/common/");
            response = httpClient.execute(httpGet);
            HttpEntity entity = response.getEntity();
            String result = EntityUtils.toString(entity);
            //此时的result是一个页面，里面有非常多的html元素，使用正则表达式去匹配到loginToken。
            //使用正则表达式去匹配到loginToken
            Pattern pattern = Pattern.compile("\"\\d+\"");
            Matcher matcher = pattern.matcher(result);
            if (matcher.find()) {
                result = matcher.group().replace("\"", "");
                return result;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void login() {
        //请求参数
        List<NameValuePair> params = new LinkedList<>();
        BasicNameValuePair param1 = new BasicNameValuePair("logintoken", getLoginToken());
        BasicNameValuePair param2 = new BasicNameValuePair("IPT_LOGINUSERNAME", userAccount);
        BasicNameValuePair param3 = new BasicNameValuePair("IPT_LOGINPASSWORD", userPassword);
        params.add(param1);
        params.add(param2);
        params.add(param3);
        //使用URL实体转换工具
        try {
            URIBuilder uriBuilder = new URIBuilder("http://pt.csust.edu.cn/meol/loginCheck.do");
            uriBuilder.setParameters(params);
            // 根据带参数的URI对象构建GET请求对象
            httpGet = new HttpGet(uriBuilder.build());
            response = httpClient.execute(httpGet);
        } catch (IOException | ParseException | URISyntaxException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取到作业提醒的页面
     *
     */
    public String getReminder() {
        httpGet = new HttpGet("http://pt.csust.edu.cn/meol/welcomepage/student/interaction_reminder.jsp");
        HttpEntity entity;
        String result = null;
        try {
            response = httpClient.execute(httpGet);
            entity = response.getEntity();
            result = EntityUtils.toString(entity);
        } catch (IOException e) {
            System.out.println("获取未读提醒失败！");
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 获取到每个课程的url
     *
     * @param reminderPage 作业提醒页面
     */
    public List<String> getLessonUrls(String reminderPage) {
        List<String> urls = new ArrayList<>();
        Pattern pattern = Pattern.compile("/lesson.+t=hw");
        Matcher matcher = pattern.matcher(reminderPage);
        String url;
        String lessId;
        while (matcher.find()) {
            url = matcher.group();
            lessId = url.split("=")[1].replace("&t", "");
            url = "http://pt.csust.edu.cn/meol/jpk/course/layout/newpage/default_demonstrate.jsp?courseId=" + lessId;
            urls.add(url);
        }
        return urls;
    }

    /**
     * 具体的某个课程页面
     *
     * @param url 课程url
     */
    public String lessonPage(String url) {
        String result = "";
        try {
            httpGet = new HttpGet(url);
            HttpEntity httpEntity;
            response = httpClient.execute(httpGet);
            httpEntity = response.getEntity();
            result = EntityUtils.toString(httpEntity);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    /**
     * 获取每个作业的Url
     *
     * @param lessonPage 某个课程的页面
     */
    public List<String> getHomeworkUrls(String lessonPage) {
        List<String> urls = new ArrayList<>();
        //先获取每个作业的id
        Pattern pattern = Pattern.compile("hwtid=\\d+");
        Matcher matcher = pattern.matcher(lessonPage);
        String url;
        while (matcher.find()) {
            url = "http://pt.csust.edu.cn/meol/common/hw/student/hwtask.view.jsp?" + matcher.group();
            urls.add(url);
        }
        return urls;
    }

    /**
     * 获取老师的姓名
     *
     * @param lessonPage 某个课程页面
     */
    public String getTeacherName(String lessonPage) {
        Pattern pattern = Pattern.compile("<span>.+</span></a>发布了新的作业");
        Matcher matcher = pattern.matcher(lessonPage);
        String name = null;
        while (matcher.find()) {
            name = matcher.group().split("[><]")[2];
        }
        return name;
    }

    /**
     * 进入到每个作业的具体页面
     *
     */
    public String homeworkPage(String url) {
        String result = null;
        try {
            httpGet = new HttpGet(url);
            response = httpClient.execute(httpGet);
            HttpEntity entity = response.getEntity();
            result = EntityUtils.toString(entity);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 获取每个作业的截止时间
     *
     * @param homeworkPage 作业具体页面
     */
    public String getDeadLine(String homeworkPage) {
        Pattern pattern = Pattern.compile("\\d+年\\d+月\\d+日");
        Matcher matcher = pattern.matcher(homeworkPage);
        String deadLine = null;
        while (matcher.find()) {
            deadLine = matcher.group();
        }
        return deadLine;
    }

    public String getHomeworkName(String homeworkPage) {
        Pattern pattern = Pattern.compile("<td>.+&nbsp;</td>");
        Matcher matcher = pattern.matcher(homeworkPage);
        String name = null;
        while (matcher.find()) {
            name = matcher.group().substring(matcher.group().indexOf('>') + 1, matcher.group().indexOf('&'));
        }
        return name;
    }

    public void close() {
        try {
            response.close();
            httpClient.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }





}
