package documentation;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestMapping;

import com.crawlerApi.api.AccountController;
import com.crawlerApi.api.AuditController;
import com.crawlerApi.api.AuditRecordController;
import com.crawlerApi.api.AuditorController;
import com.crawlerApi.api.CompetitorController;
import com.crawlerApi.api.DesignSystemController;
import com.crawlerApi.api.DomainController;
import com.crawlerApi.api.ElementController;
import com.crawlerApi.api.IdeTestExportController;
import com.crawlerApi.api.IntegrationsController;
import com.crawlerApi.api.PageController;
import com.crawlerApi.api.TestRecordController;
import com.crawlerApi.api.TestUserController;
import com.crawlerApi.api.UserInfoController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Test class to verify that all API endpoints are properly documented with OpenAPI annotations
 */
class OpenApiDocumentationTest {

    private static final List<Class<?>> CONTROLLER_CLASSES = Arrays.asList(
        AccountController.class,
        AuditController.class,
        AuditRecordController.class,
        AuditorController.class,
        CompetitorController.class,
        DesignSystemController.class,
        DomainController.class,
        ElementController.class,
        IdeTestExportController.class,
        IntegrationsController.class,
        PageController.class,
        TestRecordController.class,
        TestUserController.class,
        UserInfoController.class
    );

    @Test
    void testAllControllersHaveTagAnnotations() {
        for (Class<?> controllerClass : CONTROLLER_CLASSES) {
            assertTrue(controllerClass.isAnnotationPresent(Tag.class), 
                "Controller " + controllerClass.getSimpleName() + " should have @Tag annotation");
            
            Tag tag = controllerClass.getAnnotation(Tag.class);
            assertNotNull(tag.name(), "Tag name should not be null for " + controllerClass.getSimpleName());
            assertNotNull(tag.description(), "Tag description should not be null for " + controllerClass.getSimpleName());
        }
    }

    @Test
    void testAllEndpointsHaveOperationAnnotations() {
        for (Class<?> controllerClass : CONTROLLER_CLASSES) {
            List<Method> endpointMethods = getEndpointMethods(controllerClass);
            
            for (Method method : endpointMethods) {
                assertTrue(method.isAnnotationPresent(Operation.class), 
                    "Method " + method.getName() + " in " + controllerClass.getSimpleName() + " should have @Operation annotation");
                
                Operation operation = method.getAnnotation(Operation.class);
                assertNotNull(operation.summary(), "Operation summary should not be null for " + method.getName());
                assertNotNull(operation.description(), "Operation description should not be null for " + method.getName());
            }
        }
    }

    @Test
    void testAllEndpointsHaveApiResponsesAnnotations() {
        for (Class<?> controllerClass : CONTROLLER_CLASSES) {
            List<Method> endpointMethods = getEndpointMethods(controllerClass);
            
            for (Method method : endpointMethods) {
                assertTrue(method.isAnnotationPresent(ApiResponses.class), 
                    "Method " + method.getName() + " in " + controllerClass.getSimpleName() + " should have @ApiResponses annotation");
                
                ApiResponses apiResponses = method.getAnnotation(ApiResponses.class);
                assertTrue(apiResponses.value().length > 0, "ApiResponses should have at least one response for " + method.getName());
                
                // Check that at least one success response (200) is defined
                boolean hasSuccessResponse = false;
                for (ApiResponse response : apiResponses.value()) {
                    if ("200".equals(response.responseCode())) {
                        hasSuccessResponse = true;
                        break;
                    }
                }
                assertTrue(hasSuccessResponse, "Method " + method.getName() + " should have a 200 success response defined");
            }
        }
    }

    @Test
    void testAllEndpointsHaveRequiredResponseCodes() {
        for (Class<?> controllerClass : CONTROLLER_CLASSES) {
            List<Method> endpointMethods = getEndpointMethods(controllerClass);
            
            for (Method method : endpointMethods) {
                if (method.isAnnotationPresent(ApiResponses.class)) {
                    ApiResponses apiResponses = method.getAnnotation(ApiResponses.class);
                    
                    // Check for common required response codes
                    List<String> responseCodes = Arrays.stream(apiResponses.value())
                        .map(ApiResponse::responseCode)
                        .collect(Collectors.toList());
                    
                    // All endpoints should have 401 (authentication required)
                    assertTrue(responseCodes.contains("401"), 
                        "Method " + method.getName() + " should have 401 response code");
                }
            }
        }
    }

    @Test
    void testAllControllersHaveV1RequestMapping() {
        for (Class<?> controllerClass : CONTROLLER_CLASSES) {
            assertTrue(controllerClass.isAnnotationPresent(RequestMapping.class), 
                "Controller " + controllerClass.getSimpleName() + " should have @RequestMapping annotation");
            
            RequestMapping requestMapping = controllerClass.getAnnotation(RequestMapping.class);
            String[] paths = requestMapping.path();
            
            boolean hasV1Path = Arrays.stream(paths)
                .anyMatch(path -> path.startsWith("v1/"));
            
            assertTrue(hasV1Path, "Controller " + controllerClass.getSimpleName() + " should have v1/ path");
        }
    }

    // Helper method to get endpoint methods
    private List<Method> getEndpointMethods(Class<?> controllerClass) {
        return Arrays.stream(controllerClass.getMethods())
            .filter(method -> method.isAnnotationPresent(RequestMapping.class))
            .collect(Collectors.toList());
    }
}