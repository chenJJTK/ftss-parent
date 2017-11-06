package xyz.foxtalk.ftss.ftssspider.pageprocessor;

import org.springframework.stereotype.Service;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.pipeline.ConsolePipeline;
import us.codecraft.webmagic.processor.PageProcessor;
import xyz.foxtalk.ftss.ftssspider.util.JSHttpClient;

import java.text.MessageFormat;
import java.util.LinkedHashMap;
import java.util.List;

@Service
public class JSPageProcessor implements PageProcessor {

    private Site site = Site.me().setRetryTimes(3).setSleepTime(100);

    @Override
    public void process(Page page) {

        if (page.getUrl().regex("www.jianshu.com/p").match())
            analysisArticle(page);
        if (page.getUrl().regex("www.jianshu.com/u").match())
            analysisAuthor(page);

        if (page.getUrl().regex("www.jianshu.com/c").match())
            analysisTopic(page);
    }

    public void analysisArticle(Page page) {
        //定义抽取逻辑
        page.putField("title", page.getHtml().xpath("/html/body/div[1]/div[1]/div[1]/h1/text()").toString());
        page.putField("author", page.getHtml().xpath("/html/body/div[1]/div[1]/div[1]/div[1]/div/span/a/text()").toString());
        page.putField("text", page.getHtml().toString());
        page.putField("host", page.getUrl().regex("[A-Za-z0-9_.]+/"));
        page.putField("description", page.getHtml().xpath("/html/head/meta[@name=description]/@content"));
        page.putField("url", page.getUrl());
        //抽取作者博文目录
        String author = MessageFormat.format("https://www.jianshu.com/u/{0}",
                page.getHtml().xpath("/html/body/div[1]/div[1]/div[1]/div[1]/div/span/a/@href").toString().split("/")[2]);
        page.addTargetRequest(author);
        //抽取评论者博文目录
        String comments = MessageFormat.format("http://www.jianshu.com/notes/{0}/comments",
                page.getHtml().xpath("head/meta[@name=apple-itunes-app]").regex("/[0-9]+").regex("[0-9]+").toString());
        List<LinkedHashMap<String, Object>> comms = JSHttpClient.getCommentData(comments);
        if (comms != null)
            comms.forEach(m -> {
                String commentator = MessageFormat.format("https://www.jianshu.com/u/{0}", m.get("slug"));
                page.addTargetRequest(commentator);
            });
    }

    public void analysisAuthor(Page page) {
        if (page.getUrl().regex("page").match()) {
            page.getHtml().xpath("//a[@class=title]/@href").all().forEach(u -> {
                page.addTargetRequest("http://www.jianshu.com" + u.toString());
            });
        } else {
            //加上页码再解析
            String path = MessageFormat.format("//div[@class=meta-block]/a[@href={0}]/p/text()", page.getUrl().regex("/u/[A-Za-z0-9]+"));
            int count = Integer.valueOf(page.getHtml().xpath(path).toString());
            int p = (int) Math.ceil(count / 9);
            for (int i = 1; i <= p; i++) {
                page.addTargetRequest(page.getUrl().toString() + "?page=" + i);
            }
            //搜索作者创建的主题
            path = MessageFormat.format("http://www.jianshu.com/users/{0}/collections_and_notebooks?slug={0}",
                    page.getUrl().regex("/u/[A-Za-z0-9]+").toString().split("/")[2]);
            List<LinkedHashMap<String, Object>> topic = JSHttpClient.getTopicData(path);
            if (topic != null) {
                topic.forEach(t -> {
                    page.addTargetRequest("http://www.jianshu.com/c/" + t.get("slug"));
                });
            }
        }
    }

    public void analysisTopic(Page page) {
        if (page.getUrl().regex("page").match()) {
            page.getHtml().xpath("//a[@class=title]/@href").all().forEach(u -> {
                System.out.println(u);
                page.addTargetRequest("http://www.jianshu.com" + u.toString());
            });
        } else {
            //加上页面再解析
            int count = Integer.valueOf(page.getHtml().xpath("//div[@class=main-top]/div[@class=info]/text()").toString().split("[^0-9]+")[1]);
            int p = (int) Math.ceil(count / 10);
            for (int i = 1; i <= p; i++) {
                page.addTargetRequest(page.getUrl().toString() + "?page=" + i);
            }
            //抽取关注用户
            String path = MessageFormat.format("http://www.jianshu.com/collections/{0}/side_list",
                    page.getHtml().xpath("//div[@class=follow-button]/@props-data-collection-id").toString());
            List<LinkedHashMap<String, Object>> subscribers = JSHttpClient.getSubscriberData(path);
            if (subscribers != null) {
                subscribers.forEach(s -> {
                    page.addTargetRequest("http://www.jianshu.com/u/" + s.get("slug"));
                });
            }
        }
    }

    @Override
    public Site getSite() {
        return site;
    }

    public static void main(String[] args) {
        Spider.create(new JSPageProcessor())
                .addUrl("http://www.jianshu.com/c/d64919efac32")
                .addPipeline(new ConsolePipeline())
                .thread(5)
                .run();
    }
}

