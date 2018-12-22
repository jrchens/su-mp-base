package cn.com.simpleuse.base.service.impl;

import cn.com.simpleuse.base.service.AccessTokenService;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileCopyUtils;

import javax.sql.DataSource;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

public class AccessTokenServiceImpl implements AccessTokenService {

    private String appId;
    private String secret;
    private String token;

    private JdbcTemplate jdbcTemplate;

    private static final Logger logger = LoggerFactory.getLogger(AccessTokenServiceImpl.class);
    public static final Gson gson = new Gson();

    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public void init() {
        if (this.appId == null) {
            this.appId = jdbcTemplate.queryForObject("select conf_value from mp_conf where conf_code = ?", String.class, "MP_APP_ID");
        }
        if (this.secret == null) {
            this.secret = jdbcTemplate.queryForObject("select conf_value from mp_conf where conf_code = ?", String.class, "MP_APP_SECRET");
        }
        if (this.token == null) {
            this.token = jdbcTemplate.queryForObject("select conf_value from mp_conf where conf_code = ?", String.class, "MP_APP_TOKEN");
        }
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

//    @Scheduled(fixedDelay = 5400000)
    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void deleteAndGetAccessToken() {
        try {
            int aff = jdbcTemplate.update("update mp_access_token set is_deleted = 1,mdtime = ? where appid = ? and is_deleted = 0", DateTime.now().toDate(), getAppId());
            logger.info("update mp_access_token is_deleted = 1 affected rows: {}", aff);

            CloseableHttpClient httpclient = HttpClients.createDefault();
            try {
                HttpGet httpget = new HttpGet(String.format("https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=%s&secret=%s", getAppId(), getSecret()));

                CloseableHttpResponse response = null;
                try {
                    logger.info("start get access_token time {}", DateTime.now().toString("yyyyMMddHHmmssSSS"));
                    response = httpclient.execute(httpget);
                    logger.info("end   get access_token time {}", DateTime.now().toString("yyyyMMddHHmmssSSS"));

                    HttpEntity entity = response.getEntity();
                    String result = FileCopyUtils.copyToString(new InputStreamReader(entity.getContent()));
                    Map<String, String> map = gson.fromJson(result, new TypeToken<Map<String, String>>() {
                    }.getType());
                    String accessToken = map.get("access_token");
                    if (accessToken == null) {
                        jdbcTemplate.update("INSERT INTO mp_access_token (appid,errcode,errmsg,is_deleted,crtime) VALUES (?,?,?,?,?)", getAppId(), map.get("errcode"), map.get("errmsg"), false, DateTime.now().toDate());
                    } else {
                        double step = 0.75d;
                        int seconds = new BigDecimal(map.get("expires_in")).divide(new BigDecimal(step)).intValue();
                        Date expiresIn = DateTime.now().plusSeconds(seconds).toDate();
                        jdbcTemplate.update("INSERT INTO mp_access_token (appid,access_token,expires_time,is_deleted,crtime) VALUES (?,?,?,?,?)", getAppId(), accessToken, expiresIn, false, DateTime.now().toDate());
                    }
                } finally {
                    response.close();
                }
            } finally {
                httpclient.close();
            }
            logger.info("run deleteAndGetAccessToken task success {}", DateTime.now().toString("yyyyMMddHHmmssSSS"));
        } catch (Exception e) {
            logger.error("", e);
            throw new RuntimeException(e);
        }
    }
}
