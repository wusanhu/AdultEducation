package com.adultedu.aspect;

import com.adultedu.security.ClientUtils;
import com.adultedu.security.LoginUser;
import com.adultedu.security.SecurityUtils;
import com.adultedu.service.SystemService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperLogAspect {
    private final SystemService systemService;
    private final ClientUtils clientUtils;

    @Around("@annotation(operLog)")
    public Object around(ProceedingJoinPoint point, OperLog operLog) throws Throwable {
        long start = System.currentTimeMillis();
        boolean success = true;
        try {
            return point.proceed();
        } catch (Throwable e) {
            success = false;
            throw e;
        } finally {
            try {
                LoginUser user = SecurityUtils.currentOrNull();
                HttpServletRequest request = currentRequest();
                String ip = request == null ? "" : clientUtils.ip(request);
                String device = request == null ? "" : clientUtils.device(request);
                String method = point.getSignature().getDeclaringType().getSimpleName() + "." + point.getSignature().getName();
                systemService.writeOperLog(user, operLog.module(), operLog.content(), ip, device,
                        success ? 1 : 0, method, System.currentTimeMillis() - start);
            } catch (Exception e) {
                log.warn("操作日志写入失败: {}", e.getMessage());
            }
        }
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs == null ? null : attrs.getRequest();
    }
}
