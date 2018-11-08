//package com.ming.processor.aop;
//
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
//import org.aspectj.lang.ProceedingJoinPoint;
//import org.aspectj.lang.annotation.Around;
//import org.aspectj.lang.annotation.Aspect;
//import org.aspectj.lang.annotation.Pointcut;
//import org.springframework.stereotype.Component;
//
///**
// * 检测方法执行耗时的spring切面类
// * 使用@Aspect注解的类，Spring将会把它当作一个特殊的Bean（一个切面），也就是不对这个类本身进行动态代理
// * @author blinkfox
// * @date 2016-07-04
// */
//@Aspect
//@Component
//public class TimeInterceptor {
//
//    private static final Log LOG = LogFactory.getLog(TimeInterceptor.class);
//
//    /**
//     * 切入点
//     */
//    @Pointcut("execution( * com.ming.processor.service.OffsetService.doFilter(..))")
//    public void aopPointCut() {}
//
//    /**
//     * 统计方法耗时环绕通知
//     * @param joinPoint
//     */
//    @Around("aopPointCut()")
//    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable{
////    	 LOG.debug("logPointcut " + joinPoint + "\t");
//        long start = System.currentTimeMillis();
//        try {
//            Object result = joinPoint.proceed();
//            long end = System.currentTimeMillis();
//            LOG.info(joinPoint + "用时 : " + (end - start)/1000 + " 秒!");
//            return result;
//
//        } catch (Throwable e) {
//            long end = System.currentTimeMillis();
//            LOG.error(joinPoint + "用时 : " + (end - start)/1000 + " 秒 with exception : " + e.getMessage());
//            throw e;
//        }
//    }
//
//}
