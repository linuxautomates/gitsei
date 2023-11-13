package io.levelops.auth.utils;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.when;

public class ControllerMethodFinderTest {

    private ControllerMethodFinder controllerMethodFinder;
    @Mock
    private RequestMappingHandlerMapping handlerMapping;
    private Map<RequestMappingInfo, HandlerMethod> handlerMethods;

    @Before
    public void setUp() throws NoSuchMethodException {
        handlerMapping = Mockito.mock(RequestMappingHandlerMapping.class);
        handlerMethods = new HashMap<>();

        ControllerMethodFinderTest controllerMethodFinderTest = new ControllerMethodFinderTest();

        Class<?> myClass = ControllerMethodFinderTest.class;

        Method method1 = myClass.getMethod("dummyMethod1");
        HandlerMethod handlerMethod1 = new HandlerMethod(controllerMethodFinderTest, method1);
        RequestMappingInfo requestMappingInfo1 = RequestMappingInfo.paths("/v1/tickets_report").methods(RequestMethod.POST).build();

        Method method2 = myClass.getMethod("dummyMethod2");
        HandlerMethod handlerMethod2 = new HandlerMethod(controllerMethodFinderTest, method2);
        RequestMappingInfo requestMappingInfo2 = RequestMappingInfo.paths("/v1/org/workspaces/orgIdentifier/{orgIdentifier}/projectIdentifier/{projectIdentifier}").methods(RequestMethod.GET).build();

        Method method3 = myClass.getMethod("dummyMethod3");
        HandlerMethod handlerMethod3 = new HandlerMethod(controllerMethodFinderTest, method3);
        RequestMappingInfo requestMappingInfo3= RequestMappingInfo.paths("/v1/ous/{ou_id}/dashboards/list").methods(RequestMethod.GET).build();

        Method method4 = myClass.getMethod("dummyMethod4");
        HandlerMethod handlerMethod4 = new HandlerMethod(controllerMethodFinderTest, method4);
        RequestMappingInfo requestMappingInfo4= RequestMappingInfo.paths("/v1/org/units/list").methods(RequestMethod.POST).build();

        Method method5 = myClass.getMethod("dummyMethod5");
        HandlerMethod handlerMethod5 = new HandlerMethod(controllerMethodFinderTest, method5);
        RequestMappingInfo requestMappingInfo5= RequestMappingInfo.paths("/v1/dashboards/{dashboardid:[0-9]+}").methods(RequestMethod.POST).build();

        Method method6 = myClass.getMethod("dummyMethod6");
        HandlerMethod handlerMethod6 = new HandlerMethod(controllerMethodFinderTest, method6);
        RequestMappingInfo requestMappingInfo6= RequestMappingInfo.paths("/v1/dashboards/list").methods(RequestMethod.POST).build();

        handlerMethods.put(requestMappingInfo1, handlerMethod1);
        handlerMethods.put(requestMappingInfo2, handlerMethod2);
        handlerMethods.put(requestMappingInfo3, handlerMethod3);
        handlerMethods.put(requestMappingInfo4, handlerMethod4);
        handlerMethods.put(requestMappingInfo5, handlerMethod5);
        handlerMethods.put(requestMappingInfo6, handlerMethod6);

        when(handlerMapping.getHandlerMethods()).thenReturn(handlerMethods);
        controllerMethodFinder = new ControllerMethodFinder(handlerMapping);
        controllerMethodFinder.init();
    }

    @Test
    public void testFindControllerMethod() {

        Method result = controllerMethodFinder.findControllerMethod("/v1/tickets_report", "POST");
        Assertions.assertThat(result.getName()).isEqualTo("dummyMethod1");

        result = controllerMethodFinder.findControllerMethod("/v1/org/workspaces/orgIdentifier/11/projectIdentifier/23", "GET");
        Assertions.assertThat(result.getName()).isEqualTo("dummyMethod2");

        result = controllerMethodFinder.findControllerMethod("/v1/ous/2b728ffa-ef56-460e-bfb3-d53b5306b616/dashboards/list", "GET");
        Assertions.assertThat(result.getName()).isEqualTo("dummyMethod3");

        result = controllerMethodFinder.findControllerMethod("/v1/org/units/list", "POST");
        Assertions.assertThat(result.getName()).isEqualTo("dummyMethod4");

        result = controllerMethodFinder.findControllerMethod("/v1/dashboards/12", "POST");
        Assertions.assertThat(result.getName()).isEqualTo("dummyMethod5");

        result = controllerMethodFinder.findControllerMethod("/v1/dashboards/list", "POST");
        Assertions.assertThat(result.getName()).isEqualTo("dummyMethod6");

    }

    @RequestMapping(method = RequestMethod.POST, value = "v1/tickets_report", produces = "application/json")
    public void dummyMethod1(){
    }

    @RequestMapping(method = RequestMethod.GET, value = "v1/org/workspaces/orgIdentifier/{orgIdentifier}/projectIdentifier/{projectIdentifier}", produces = "application/json")
    public void dummyMethod2(){
    }

    @RequestMapping(method = RequestMethod.GET, value = "v1/{ou_id}/dashboards/list", produces = "application/json")
    public void dummyMethod3(){
    }

    @PostMapping(path = "/v1/org/units/list", produces = "application/json")
    public void dummyMethod4(){
    }

    @PostMapping(path = "/v1/dashboards/{dashboardid:[0-9]+}", produces = "application/json")
    public void dummyMethod5(){
    }

    @PostMapping(path = "/v1/dashboards/list", produces = "application/json")
    public void dummyMethod6(){
    }
}

