package xyz.foxtalk.ftss.ftssspider.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;

import java.net.URI;
import java.text.MessageFormat;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

public class JSHttpClient {
    HttpClient client = HttpClients.createDefault();

    //http://www.jianshu.com/notes/17414513/comments
    public static List<LinkedHashMap<String, Object>> getCommentData(String url) {
        try {
            JSHttpClient oClient = new JSHttpClient();
            List<LinkedHashMap<String, Object>> result = new LinkedList<>();
            HttpGet get = new HttpGet(url);
            get.addHeader("Accept", "*/*");
            HttpResponse res = oClient.client.execute(get);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(res.getEntity().getContent());
            if (!node.get("comment_exist").asBoolean()) {
                return null;
            }
            int page = 1;
            do {
                String uri = MessageFormat.format(url + "?page={0}", page++);
                get.setURI(new URI(uri));
                res = oClient.client.execute(get);
                node = mapper.readTree(res.getEntity().getContent());
                result.addAll(mapper.readValue(node.get("comments").toString(), List.class));
            } while (node.get("total_pages").asInt() > node.get("page").asInt());
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    //http://www.jianshu.com/users/e210c8fa704f/collections_and_notebooks?slug=e210c8fa704f
    public static List<LinkedHashMap<String, Object>> getTopicData(String url) {
        try {
            JSHttpClient oClient = new JSHttpClient();
            HttpGet get = new HttpGet(url);
            get.addHeader("Accept", "*/*");
            HttpResponse res = oClient.client.execute(get);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(res.getEntity().getContent());
            List<LinkedHashMap<String, Object>> result = mapper.readValue(node.get("own_collections").toString(), List.class);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static List<LinkedHashMap<String, Object>> getSubscriberData(String url) {
        try {
            JSHttpClient oClient = new JSHttpClient();
            HttpGet get = new HttpGet(url);
            get.addHeader("Accept", "*/*");
            HttpResponse res = oClient.client.execute(get);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(res.getEntity().getContent());
            List<LinkedHashMap<String, Object>> result = mapper.readValue(node.get("subscribers").toString(), List.class);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}