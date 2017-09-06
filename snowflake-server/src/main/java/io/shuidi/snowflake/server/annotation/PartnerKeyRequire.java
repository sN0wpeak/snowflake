package io.shuidi.snowflake.server.annotation;

import java.lang.annotation.*;

/**
 * Author: Alvin Tian
 * Date: 2017/9/5 13:52
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PartnerKeyRequire {

}
