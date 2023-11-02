package radar.spring.auth.managementportal

import org.radarbase.auth.authorization.AuthorizationOracle
import org.radarbase.auth.authorization.EntityDetails
import org.radarbase.auth.authorization.Permission
import org.radarbase.auth.token.RadarToken
import org.slf4j.LoggerFactory
import radar.spring.auth.common.Authorization
import radar.spring.auth.common.PermissionOn

class ManagementPortalAuthorization(val oracle: AuthorizationOracle) : Authorization<RadarToken> {
    override suspend fun hasPermission(
        token: RadarToken,
        permission: String,
        entity: String,
        permissionOn: PermissionOn?,
        project: String?,
        user: String?,
        source: String?
    ): Boolean {
        val mpPermission =
            Permission.of(
                Permission.Entity.valueOf(entity),
                Permission.Operation.valueOf(permission)
            )
        return when (permissionOn) {
            PermissionOn.PROJECT -> checkPermissionOnProject(token, mpPermission, project)
            PermissionOn.SUBJECT -> checkPermissionOnSubject(token, mpPermission, project, user)
            PermissionOn.SOURCE ->
                checkPermissionOnSource(
                    token,
                    mpPermission,
                    project,
                    user,
                    source
                )
            else ->
                oracle.hasPermission(
                    token,
                    mpPermission,
                    EntityDetails(
                        project,
                        user,
                        source
                    )
                )
        }
    }

    override fun hasRole(
        token: RadarToken,
        project: String?,
        role: String?
    ): Boolean {
        if (role.isNullOrBlank()) {
            return true
        }
        if (project.isNullOrBlank()) {
            logger.warn("Project must be specified when checking a role.")
            return false
        }
        return token.roles.asSequence()
            .filter { it.referent == project }
            .any { it.authority == role }
    }

    override fun hasScopes(
        token: RadarToken,
        scopes: Array<String>
    ): Boolean {
        return token.scopes.containsAll(scopes.toList())
    }

    override fun hasAudiences(
        token: RadarToken,
        audiences: Array<String>
    ): Boolean {
        return token.audience.containsAll(audiences.toList())
    }

    override fun hasGrantTypes(
        token: RadarToken,
        grantTypes: Array<String>
    ): Boolean {
        if (grantTypes.isEmpty()) {
            return true
        }
        return grantTypes.contains(token.grantType)
    }

    private suspend fun checkPermissionOnProject(
        token: RadarToken,
        mpPermission: Permission,
        project: String?
    ): Boolean {
        if (project.isNullOrBlank()) {
            logger.warn(
                "The project must be specified when checking permissions on PROJECT."
            )
            return false
        }
        return oracle.hasPermission(
            token,
            mpPermission,
            EntityDetails(project = project)
        )
    }

    private suspend fun checkPermissionOnSubject(
        token: RadarToken,
        mpPermission: Permission,
        project: String?,
        user: String?
    ): Boolean {
        if (project.isNullOrBlank() || user.isNullOrBlank()) {
            logger.warn(
                "The project and subject must be specified when checking permissions on SUBJECT."
            )
            return false
        }
        return oracle.hasPermission(
            token,
            mpPermission,
            EntityDetails(user = user)
        )
    }

    private suspend fun checkPermissionOnSource(
        token: RadarToken,
        mpPermission: Permission,
        project: String?,
        user: String?,
        source: String?,
    ): Boolean {
        if (project.isNullOrBlank() || user.isNullOrBlank() || source.isNullOrBlank()) {
            logger.warn(
                "The project, subject and source must be specified when checking permissions on " +
                    "SOURCE."
            )
            return false
        }
        return oracle.hasPermission(
            token,
            mpPermission,
            EntityDetails(user = user, source = source)
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ManagementPortalAuthorization::class.java)
    }
}
