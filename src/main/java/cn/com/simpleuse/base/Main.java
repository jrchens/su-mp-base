package cn.com.simpleuse.base;

import cn.com.simpleuse.base.service.AccessTokenService;
import cn.com.simpleuse.base.service.impl.AccessTokenServiceImpl;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Main {

    public static void main(String[] args) {
        ApplicationContext context =
                new ClassPathXmlApplicationContext(new String[] {"classpath:applicationContext.xml"});

//        AccessTokenService accessTokenService = context.getBean(AccessTokenServiceImpl.class);
//        accessTokenService.deleteAndGetAccessToken();

    }
}
