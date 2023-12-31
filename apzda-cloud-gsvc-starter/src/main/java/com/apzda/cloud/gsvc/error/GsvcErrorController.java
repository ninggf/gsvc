package com.apzda.cloud.gsvc.error;

import com.apzda.cloud.gsvc.config.ServiceConfigProperties;
import com.apzda.cloud.gsvc.utils.ResponseUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.web.ErrorProperties;
import org.springframework.boot.autoconfigure.web.servlet.error.BasicErrorController;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorViewResolver;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author fengz
 */
@Controller
@RequestMapping("${server.error.path:${error.path:/error}}")
public class GsvcErrorController extends BasicErrorController {

    private final ErrorAttributes errorAttributes;

    public GsvcErrorController(ErrorAttributes errorAttributes, ErrorProperties errorProperties,
            List<ErrorViewResolver> errorViewResolvers) {
        super(errorAttributes, errorProperties, errorViewResolvers);
        this.errorAttributes = errorAttributes;
    }

    @RequestMapping(produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView errorHtml(HttpServletRequest request, HttpServletResponse response) {
        HttpStatus status = getStatus(request);
        val webRequest = new ServletWebRequest(request);

        Throwable error = errorAttributes.getError(webRequest);
        if (error != null) {
            while (error instanceof ServletException && error.getCause() != null) {
                error = error.getCause();
            }
            val loginUrl = ResponseUtils.getLoginUrl(ResponseUtils.mediaTypes(request));

            if (StringUtils.isNotBlank(loginUrl)) {
                val redirectView = new ModelAndView(new RedirectView(loginUrl));
                if (error instanceof HttpStatusCodeException codeException
                        && codeException.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                    return redirectView;
                }
            }
        }

        Map<String, Object> model = Collections
            .unmodifiableMap(getErrorAttributes(webRequest, getErrorAttributeOptions(request, MediaType.TEXT_HTML)));
        response.setStatus(status.value());
        ModelAndView modelAndView = resolveErrorView(request, response, status, model);
        return (modelAndView != null) ? modelAndView : new ModelAndView("error", model);
    }

    @Override
    @RequestMapping
    public ResponseEntity<Map<String, Object>> error(HttpServletRequest request) {
        HttpStatus status = getStatus(request);
        if (status == HttpStatus.NO_CONTENT) {
            return new ResponseEntity<>(status);
        }
        val webRequest = new ServletWebRequest(request);
        Map<String, Object> body = getErrorAttributes(webRequest, getErrorAttributeOptions(request, MediaType.ALL));
        return ResponseEntity.status(status).body(body);
    }

    protected Map<String, Object> getErrorAttributes(ServletWebRequest request, ErrorAttributeOptions options) {
        return this.errorAttributes.getErrorAttributes(request, options);
    }

}
