# RADAR Spring

Collection of common code for using in spring framework based applications for the RADAR-base platform.


## radar-spring-auth

This library provides functionality to add authorisation in any spring based application. Currently, this is provided using the Management portal and radar-auth library.
The library is written in Kotlin but is fully compatible with java.

### Usage
This library uses AOP (Aspect Oriented Programming) and hence requires an AOP processor to be present in the application's runtime.
Since we are using this in spring applications, we can use `spring-aop`. So add the following to your application's `build.gradle` file.

```groovy
    // AOP
    runtimeOnly(group: 'org.springframework', name: 'spring-aop', version: '5.2.4.RELEASE')
    implementation(group: 'org.radarbase', name: 'radar-spring-auth', version: '1.0.0-SNAPSHOT')
```

Then we need to add our Authorisation Aspect to the spring context as a bean.

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import radar.spring.auth.common.AuthAspect;
import radar.spring.auth.config.ManagementPortalAuthProperties;
import radar.spring.auth.managementportal.ManagementPortalAuthValidator;
import radar.spring.auth.managementportal.ManagementPortalAuthorization;

@Configuration
@EnableAspectJAutoProxy
public class AuthConfig {

  private String baseUrl = "<your-management-portal-url>";

  private String resourceName = "<your-resource-name>";

  @Bean
  public ManagementPortalAuthProperties getAuthProperties() {
    return new ManagementPortalAuthProperties(baseUrl, resourceName);
  }

  @Bean
  ManagementPortalAuthValidator getAuthValidator(
      @Autowired ManagementPortalAuthProperties managementPortalAuthProperties) {
    return new ManagementPortalAuthValidator(managementPortalAuthProperties);
  }

  @Bean
  ManagementPortalAuthorization getAuthorization() {
    return new ManagementPortalAuthorization();
  }

  @Bean
  AuthAspect getAuthAspect(
      @Autowired ManagementPortalAuthValidator authValidator,
      @Autowired ManagementPortalAuthorization authorization) {
    return new AuthAspect<>(authValidator, authorization);
  }
}
```

Although, we only need `AuthAspect` as a bean, we declare it's dependencies as a bean too, so they can be reused in the application using `Autowired`.

Now, we add the `Authorized` annotation to our method that we want to authorize for (these are usually spring `Controller` methods).

```java
  @Authorized(permission = "READ", entity = "SUBJECT", permissionOn = PermissionOn.SUBJECT)
  @GetMapping(
      "/"
          + "projects"
          + "/"
          + "{projectId}"
          + "/"
          + "users"
          + "/"
          + "{subjectId}")
  public ResponseEntity<FcmUserDto> getUsersUsingProjectIdAndSubjectId(
      @Valid @PathVariable String projectId, @Valid @PathVariable String subjectId) {

    return ResponseEntity.ok(
        this.userService.getUsersByProjectIdAndSubjectId(projectId, subjectId));
  }
```

Various other conditions to verify can be provided using the `Authorized` annotation. For a full set, take a look at the [annotation class](./radar-spring-auth/src/main/kotlin/radar/spring/auth/common/Authorization.kt)
For a full set of `permission` currently accepted, see the [Operation](https://github.com/RADAR-base/ManagementPortal/blob/f104a91c3816d212c1611cb2e54c6201bc6ffa48/radar-auth/src/main/java/org/radarcns/auth/authorization/Permission.java#L37) enum in radar-auth library.
For a full set of `entity` currently accepted, see the [Entity](https://github.com/RADAR-base/ManagementPortal/blob/f104a91c3816d212c1611cb2e54c6201bc6ffa48/radar-auth/src/main/java/org/radarcns/auth/authorization/Permission.java#L20) enum in radar-auth library.

#### Parameters Required

Currently, supported `permissionOn` are Project, Subject, Source and Global/Default. 
* If checking `permissionOn` `PermissionOn.PROJECT`, then you need to supply `String projectId` argument as a parameter of the method annotated with `Authorized`.
* If checking `permissionOn` `PermissionOn.SUBJECT`, then you need to supply `String projectId` and `String subjectId` arguments as a parameter of the method annotated with `Authorized`.
* If checking `permissionOn` `PermissionOn.SOURCE`, then you need to supply `String projectId`,`String subjectId` and `String sourceId` arguments as a parameter of the method annotated with `Authorized`.

These conditions are shown in the above example.

The `Authorized` annotation adds a request attribute named `radar_token` (present as `AuthAspect.TOKEN_KEY` constant property) which contains the instance of `org.radarbase.auth.RadarToken` from the radar-auth library. This can be used inside the method body to perform additional authorization. For example, this can be used to filter the projects a token has access to as follows-

```java
import java.util.Optional;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;

import radar.spring.auth.common.Authorization;
import radar.spring.auth.common.Authorized;
import radar.spring.auth.common.PermissionOn;
import radar.spring.auth.exception.AuthorizationFailedException;

import org.radarcns.auth.token.RadarToken;

import org.radarbase.appserver.dto.ProjectDto;
import org.radarbase.appserver.dto.ProjectDtos;
import org.radarbase.appserver.service.ProjectService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

@RestController
public class RadarProjectController {
  // Your project Service
  private transient ProjectService projectService;

  private transient Authorization<RadarToken> authorization;

  public RadarProjectController(
      ProjectService projectService, Optional<Authorization<RadarToken>> authorization) {
    this.projectService = projectService;
    this.authorization = authorization.orElse(null);
  }


  @Authorized(permission = "READ", entity = "PROJECT")
  @GetMapping("/" + "projects")
  public ResponseEntity<ProjectDtos> getAllProjects(HttpServletRequest request) {

    ProjectDtos projectDtos = this.projectService.getAllProjects();
    if (authorization != null) {
      RadarToken token = (RadarToken) request.getAttribute(AuthAspect.TOKEN_KEY);
      ProjectDtos finalProjectDtos =
          new ProjectDtos()
              .setProjects(
                  projectDtos.getProjects().stream()
                      .filter(
                          project ->
                              authorization.hasPermission(
                                  token,
                                  "READ",
                                  "PROJECT",
                                  PermissionOn.PROJECT,
                                  project.getProjectId(),
                                  null,
                                  null))
                      .collect(Collectors.toList()));
      return ResponseEntity.ok(finalProjectDtos);
    } else {
      // If not authorization object if present, means authorization is disabled.
      // Remember how we added this as a bean initially.
      return ResponseEntity.ok(projectDtos);
    }
  }
}
```


### Extending

The various parts of the application can be extended as required. Take a look at [AuthValidator](./radar-spring-auth/src/main/kotlin/radar/spring/auth/common/AuthValidator.kt) and [Authorization](./radar-spring-auth/src/main/kotlin/radar/spring/auth/common/Authorization.kt) interfaces which can be used to implement a new authorization. These can then be used to instantiate the `AuthAspect` to enable them.
You can also add another Aspect as per your requirements in your own project and add it as a Bean in spring to start using it just like the `AuthAspect` from this library.


The [required parameter](#parameters-required) names can also be changed as per your requirements apart from the default ones mentioned above. You can even specify multiple names as an array. These will need to be added when creating the `AuthAspect`. For example,

```java
...
  @Bean
  AuthAspect getAuthAspect(
      @Autowired ManagementPortalAuthValidator authValidator,
      @Autowired ManagementPortalAuthorization authorization) {
    return new AuthAspect<>(
      authValidator,
      authorization, 
      new String[]{"projectId", "projectName", "project"},
      new String[]{"subjectId", "login"}, 
      new String[]{"sourceId", "source"}
    );
  }
...
```

But Note that while you can modify the name of the parameters according to you liking, their type must always be `String`.
